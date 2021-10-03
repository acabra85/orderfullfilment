package com.acabra.orderfullfilment.orderserver.core;

import com.acabra.orderfullfilment.orderserver.kitchen.KitchenLog;
import com.acabra.orderfullfilment.orderserver.config.OrderServerConfig;
import com.acabra.orderfullfilment.orderserver.core.executor.NoMoreOrdersMonitor;
import com.acabra.orderfullfilment.orderserver.core.executor.OutputEventHandler;
import com.acabra.orderfullfilment.orderserver.core.executor.SafeTask;
import com.acabra.orderfullfilment.orderserver.core.executor.SchedulerExecutorAssistant;
import com.acabra.orderfullfilment.orderserver.courier.CourierDispatchService;
import com.acabra.orderfullfilment.orderserver.event.*;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenService;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Service
public class OrderProcessor implements Closeable, ApplicationContextAware {
    /**
     * This is the main application service, it handles processing of orders
     * provides a public method close to finish threads consuming order events.
     */
    public static final long MONITOR_START_DELAY_MILLIS = 5000L;

    private final CourierDispatchService courierService;
    private final KitchenService kitchenService;
    private final MetricsProcessor metricsProcessor;
    private final SchedulerExecutorAssistant schedulerAssistant;
    private ApplicationContext context;
    private final CompletableFuture<Void> completedHandle = new CompletableFuture<>();
    private final KitchenLog kLog = KitchenLog.get();

    public OrderProcessor(OrderServerConfig orderServerConfig,
                          CourierDispatchService courierService,
                          KitchenService kitchenService,
                          @Qualifier("order_handler") OutputEventPublisher orderHandler,
                          @Qualifier("notification_deque") Queue<OutputEvent> deque,
                          SchedulerExecutorAssistant scheduler) {
        this.metricsProcessor = new MetricsProcessor();
        this.courierService = courierService;
        this.kitchenService = kitchenService;
        this.schedulerAssistant = scheduler;

        //register public notification queue
        this.courierService.registerNotificationDeque(deque);
        this.kitchenService.registerNotificationDeque(deque);
        orderHandler.registerNotificationDeque(deque);

        //schedule tasks
        scheduleTasks(orderServerConfig, deque);
    }

    private void scheduleTasks(OrderServerConfig config, Queue<OutputEvent> deque) {
        int maxRetries = config.getPollingMaxRetries();
        Consumer<OutputEvent> dispatchOutputEvent = this::dispatchOutputEvent;
        Supplier<Boolean> hasPendingDeliveryOrders = this::hasPendingDeliveryOrders;
        SafeTask outputEventTask = new OutputEventHandler(deque, dispatchOutputEvent);
        SafeTask noMoreOrdersTask = new NoMoreOrdersMonitor(maxRetries, hasPendingDeliveryOrders, deque);

        this.schedulerAssistant.scheduleAtFixedRate(noMoreOrdersTask, MONITOR_START_DELAY_MILLIS,
                config.getPeriodShutDownMonitorMillis());
        this.schedulerAssistant.scheduleAtFixedRate(outputEventTask, 0, config.getPollingTimeMillis());
    }

    private boolean hasPendingDeliveryOrders() {
        MetricsProcessor.DeliveryMetricsSnapshot snapshot = this.metricsProcessor.snapshot();
        return snapshot.totalOrdersPrepared - snapshot.totalOrdersDelivered > 0;
    }

    private void processOrder(final OrderReceivedEvent orderReceived) {
        DeliveryOrder order = orderReceived.order;
        final long reservationId = kitchenService.provideReservationId(order);
        String orderReceivedMsg = String.format("[EVENT] order received: orderId[%s] prepTime:[%s]ms name:%s", reservationId,
                orderReceived.order.prepTime, orderReceived.order.name);
        kLog.append(orderReceived.createdAt, orderReceivedMsg);
        Optional<Integer> courierDispatched = courierService.dispatchRequest(order, reservationId, orderReceived.createdAt);
        if(courierDispatched.isPresent()) {
            metricsProcessor.acceptOrderPrepareRequest();
            kitchenService.prepareMeal(reservationId, orderReceived.createdAt);
            return;
        }
        kLog.append("No couriers available to deliver the order ... cancelling cooking reservation");
        kitchenService.cancelCookReservation(reservationId);
    }

    private void dispatchOutputEvent(final OutputEvent outputEvent) {
        switch (outputEvent.getType()) {
            case ORDER_RECEIVED:
                metricsProcessor.acceptOrderReceived();
                OrderReceivedEvent orderReceivedEvent = (OrderReceivedEvent) outputEvent;
                processOrder(orderReceivedEvent);
                break;
            case COURIER_DISPATCHED:
                CourierDispatchedEvent courierDispatchedEvent = (CourierDispatchedEvent) outputEvent;
                String dispatchedMsg = String.format("[EVENT] courier dispatched: id[%s] estimated travel time [%s]ms",
                        courierDispatchedEvent.courierId, courierDispatchedEvent.estimatedTravelTime);
                kLog.append(outputEvent.createdAt, dispatchedMsg);
                courierService.processCourierDispatchedEvent(courierDispatchedEvent);
                break;
            case ORDER_PREPARED:
                OrderPreparedEvent orderPreparedEvent = (OrderPreparedEvent) outputEvent;
                String preparedMsg = String.format("[EVENT] order prepared: mealId[%s], orderId[%s]",
                        orderPreparedEvent.kitchenReservationId, orderPreparedEvent.deliveryOrderId);
                kLog.append(outputEvent.createdAt, preparedMsg);
                courierService.processOrderPrepared(orderPreparedEvent);
                break;
            case COURIER_ARRIVED:
                CourierArrivedEvent courierArrivedEvent = (CourierArrivedEvent) outputEvent;
                String arrivedMsg = String.format("[EVENT] courier arrived id[%s], for pickup",
                        courierArrivedEvent.courierId);
                kLog.append(outputEvent.createdAt, arrivedMsg);
                courierService.processCourierArrived(courierArrivedEvent);
                break;
            case ORDER_PICKED_UP:
                OrderPickedUpEvent orderPickedUpEvent = (OrderPickedUpEvent) outputEvent;
                String pickedUpMsg = String.format("[EVENT] order picked up: orderId[%s] courierId[%s]",
                        orderPickedUpEvent.mealOrderId, orderPickedUpEvent.courierId);
                kLog.append(outputEvent.createdAt, pickedUpMsg);
                reportPickupMetrics(orderPickedUpEvent);
                processOrderPickedUp(orderPickedUpEvent);
                break;
            case ORDER_DELIVERED:
                OrderDeliveredEvent orderDeliveredEvent = (OrderDeliveredEvent) outputEvent;
                String deliveredMsg = String.format("[EVENT] order delivered: orderId[%s] by courierId[%s]",
                        orderDeliveredEvent.mealOrderId, orderDeliveredEvent.courierId);
                kLog.append(outputEvent.createdAt, deliveredMsg);
                courierService.processOrderDelivered(orderDeliveredEvent);
                break;
            case NO_PENDING_ORDERS:
                reportAverageMetrics(outputEvent);
                this.close();
                break;
            default:
                throw new UnsupportedOperationException("Event not recognized by the system" + outputEvent.type);
        }
    }

    private void processOrderPickedUp(OrderPickedUpEvent orderPickedUpEvent) {
        //emulate instant delivery TODO: [TRACKER-TICKET-SYSTEM-1235 implement real delivery]
        this.dispatchOutputEvent(OrderDeliveredEvent.of(orderPickedUpEvent));
    }

    private void reportPickupMetrics(OrderPickedUpEvent orderPickedUpEvent) {
        this.metricsProcessor.acceptFoodWaitTime(orderPickedUpEvent.foodWaitTime);
        this.metricsProcessor.acceptCourierWaitTime(orderPickedUpEvent.courierWaitTime);
        String message = String.format("[METRICS] Food wait time: [%s]ms - Courier wait time [%s]ms, orderId[%s]",
                orderPickedUpEvent.foodWaitTime, orderPickedUpEvent.courierWaitTime, orderPickedUpEvent.mealOrderId);
        kLog.append(orderPickedUpEvent.createdAt, message);
    }

    private void reportAverageMetrics(OutputEvent outputEvent) {
        MetricsProcessor.DeliveryMetricsSnapshot snapshot = this.metricsProcessor.snapshot();
        String message = String.format("[METRICS] Avg. Food Wait Time: [%.4f]ms, Avg Courier Wait Time [%.4f]ms, " +
                        "Total Orders Received %s, Total Orders Delivered %s", snapshot.avgFoodWaitTime,
                snapshot.avgCourierWaitTime, snapshot.totalOrdersReceived, snapshot.totalOrdersDelivered);
        kLog.append(outputEvent.createdAt, message);
    }

    @Override
    public void close() {
        if (!this.completedHandle.isDone()) {
            kLog.append("[SYSTEM] queue processing shutting down, no orders remaining");
            this.schedulerAssistant.shutdown();
            this.courierService.shutdown();
            this.kitchenService.shutdown();
            this.completedHandle.complete(null);
            if(null != this.context) {
                System.exit(SpringApplication.exit(this.context, () -> 0));
            }
            kLog.printLog();
        }
    }

    @Bean("notification_deque")
    public static Queue<OutputEvent> buildNotificationDeque() {
        return new PriorityBlockingQueue<>();
    }

    public MetricsProcessor.DeliveryMetricsSnapshot getMetricsSnapshot() {
        return this.metricsProcessor.snapshot();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }

    public CompletableFuture<Void> getCompletedHandle() {
        return this.completedHandle;
    }

}
