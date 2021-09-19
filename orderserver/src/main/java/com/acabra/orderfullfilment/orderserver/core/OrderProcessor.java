package com.acabra.orderfullfilment.orderserver.core;

import com.acabra.orderfullfilment.orderserver.config.OrderServerConfig;
import com.acabra.orderfullfilment.orderserver.core.executor.NoMoreOrdersMonitor;
import com.acabra.orderfullfilment.orderserver.core.executor.SchedulerExecutorAssistant;
import com.acabra.orderfullfilment.orderserver.core.executor.OutputEventHandler;
import com.acabra.orderfullfilment.orderserver.core.executor.SafeTask;
import com.acabra.orderfullfilment.orderserver.courier.CourierDispatchService;
import com.acabra.orderfullfilment.orderserver.event.*;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenService;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Service
@Slf4j
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

    public OrderProcessor(OrderServerConfig orderServerConfig,
                          CourierDispatchService courierService,
                          KitchenService kitchenService,
                          @Qualifier("order_handler") OutputEventPublisher orderHandler,
                          @Qualifier("notification_deque") Deque<OutputEvent> deque,
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

    private void scheduleTasks(OrderServerConfig config, Deque<OutputEvent> deque) {
        int maxRetries = config.getPollingMaxRetries();
        Consumer<OutputEvent> dispatchOutputEvent = this::dispatchOutputEvent;
        Supplier<Boolean> hasPendingDeliveryOrders = this::hasPendingDeliveryOrders;
        SafeTask outputEventTask = new OutputEventHandler(deque, dispatchOutputEvent);
        SafeTask noMoreOrdersTask = new NoMoreOrdersMonitor(maxRetries, hasPendingDeliveryOrders, deque);

        this.schedulerAssistant.scheduleAtFixedRate(noMoreOrdersTask, MONITOR_START_DELAY_MILLIS,
                config.getPeriodShutDownMonitorSeconds());
        this.schedulerAssistant.scheduleAtFixedRate(outputEventTask, 0, config.getPollingTimeMillis());
    }

    private boolean hasPendingDeliveryOrders() {
        MetricsProcessor.DeliveryMetricsSnapshot snapshot = this.metricsProcessor.snapshot();
        return snapshot.totalOrdersPrepared - snapshot.totalOrdersDelivered > 0;
    }

    private CompletableFuture<Boolean> processOrder(final OrderReceivedEvent orderReceived) {
        DeliveryOrder order = orderReceived.order;
        final long reservationId = kitchenService.provideReservationId(order);
        CompletableFuture<Optional<Integer>> courierDispatch = CompletableFuture
                .supplyAsync(() -> courierService.dispatchRequest(order, reservationId));
        return courierDispatch.thenApply(courierId -> {
            if(courierId.isPresent()) {
                metricsProcessor.acceptOrderPrepareRequest();
                kitchenService.prepareMeal(reservationId);
                return true;
            } else {
                log.info("No couriers available to deliver the order ... cancelling cooking reservation");
                kitchenService.cancelCookReservation(reservationId);
                return false;
            }
        });
    }

    private void dispatchOutputEvent(final OutputEvent outputEvent) {
        switch (outputEvent.getType()) {
            case ORDER_RECEIVED:
                metricsProcessor.acceptOrderReceived();
                OrderReceivedEvent orderReceivedEvent = (OrderReceivedEvent) outputEvent;
                log.info("[EVENT] order received : {} prepTime:{} name:{} at: {}" , orderReceivedEvent.order.id,
                        orderReceivedEvent.order.prepTime, orderReceivedEvent.order.name,
                        KitchenClock.formatted(orderReceivedEvent.createdAt));
                processOrder(orderReceivedEvent).join();
                break;
            case COURIER_DISPATCHED:
                CourierDispatchedEvent courierDispatchedEvent = (CourierDispatchedEvent) outputEvent;
                log.info("[EVENT] courier dispatched: id[{}] estimated travel time [{}]ms",
                        courierDispatchedEvent.courierId, courierDispatchedEvent.estimatedTravelTime);
                courierService.processCourierDispatchedEvent(courierDispatchedEvent);
                break;
            case ORDER_PREPARED:
                OrderPreparedEvent orderPreparedEvent = (OrderPreparedEvent) outputEvent;
                log.info("[EVENT] order prepared: mealId[{}], orderId[{}] at {}", orderPreparedEvent.kitchenReservationId,
                    orderPreparedEvent.deliveryOrderId, KitchenClock.formatted(orderPreparedEvent.createdAt));
                courierService.processOrderPrepared(orderPreparedEvent);
                break;
            case COURIER_ARRIVED:
                CourierArrivedEvent courierArrivedEvent = (CourierArrivedEvent) outputEvent;
                log.info("[EVENT] courier arrived id[{}], for pickup at {}ms", courierArrivedEvent.courierId,
                        KitchenClock.formatted(courierArrivedEvent.createdAt));
                courierService.processCourierArrived(courierArrivedEvent);
                break;
            case ORDER_PICKED_UP:
                OrderPickedUpEvent orderPickedUpEvent = (OrderPickedUpEvent) outputEvent;
                log.info("[EVENT] order picked up: orderId[{}] courierId[{}] at {}",
                        orderPickedUpEvent.mealOrderId, orderPickedUpEvent.courierId,
                        KitchenClock.formatted(orderPickedUpEvent.createdAt));
                reportPickupMetrics(orderPickedUpEvent);
                processOrderPickedUp(orderPickedUpEvent);
                break;
            case ORDER_DELIVERED:
                OrderDeliveredEvent orderDeliveredEvent = (OrderDeliveredEvent) outputEvent;
                log.info("[EVENT] order delivered: orderId[{}] by courierId[{}] at {}",
                        orderDeliveredEvent.mealOrderId, orderDeliveredEvent.courierId,
                        KitchenClock.formatted(orderDeliveredEvent.createdAt));
                courierService.processOrderDelivered(orderDeliveredEvent);
                break;
            case NO_PENDING_ORDERS:
                reportAverageMetrics();
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
        log.info("[METRICS] Food wait time: {}ms - Courier wait time {}ms, orderId [{}]", orderPickedUpEvent.foodWaitTime,
                orderPickedUpEvent.courierWaitTime, orderPickedUpEvent.mealOrderId);
    }

    private void reportAverageMetrics() {
        MetricsProcessor.DeliveryMetricsSnapshot snapshot = this.metricsProcessor.snapshot();
        log.info(String.format("[METRICS] Avg. Food Wait Time: [%.4f]ms, Avg Courier Wait Time [%.4f]ms, " +
                        "Total Orders Received {}, Total Orders Delivered {}", snapshot.avgFoodWaitTime,
                snapshot.avgFoodWaitTime), snapshot.totalOrdersReceived, snapshot.totalOrdersDelivered);
    }

    @Override
    public void close() {
        if (!this.schedulerAssistant.isOrdersMonitorTerminated()) {
            log.info("[SYSTEM] queue processing shutting down, no orders remaining");
            this.schedulerAssistant.shutdown();
            this.courierService.shutdown();
            this.kitchenService.shutdown();
            if(null != this.context) {
                System.exit(SpringApplication.exit(this.context, () -> 0));
            }
            this.completedHandle.complete(null);
        }
    }

    @Bean("notification_deque")
    public static Deque<OutputEvent> buildNotificationDeque() {
        return new ConcurrentLinkedDeque<>();
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
