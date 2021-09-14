package com.acabra.orderfullfilment.orderserver.core;

import com.acabra.orderfullfilment.orderserver.config.OrderServerConfig;
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

@Service
@Slf4j
public class OrderProcessor implements Closeable, ApplicationContextAware {
    /**
     * This is the main application service, it handles processing of orders
     * provides a public method close to finish threads consuming order events.
     */

    private final CourierDispatchService courierService;
    private final KitchenService kitchenService;
    private final MetricsProcessor metricsProcessor;
    private final Deque<OutputEvent> deque;
    private final ExecutorService eventHandlerExecutor;
    private final long pollingTimeMillis;
    private final ExecutorService noMoreOrdersMonitor;
    private ApplicationContext context;

    public OrderProcessor(OrderServerConfig orderServerConfig,
                          CourierDispatchService courierService,
                          KitchenService kitchenService,
                          @Qualifier("order_handler") OutputEventPublisher orderHandler,
                          @Qualifier("notification_deque") Deque<OutputEvent> deque) {
        this.metricsProcessor = new MetricsProcessor();
        this.deque = deque;
        this.pollingTimeMillis = orderServerConfig.getPollingTimeMillis();
        this.eventHandlerExecutor = startOutputEventProcessors(orderServerConfig.getThreadCount(), deque);
        this.noMoreOrdersMonitor = startNoMoreOrdersMonitor(orderServerConfig.getPeriodShutDownMonitorSeconds(),
                orderServerConfig.getPollingMaxRetries());
        this.courierService = courierService;
        this.kitchenService = kitchenService;
        //register public notification queue
        this.courierService.registerNotificationDeque(deque);
        this.kitchenService.registerNotificationDeque(deque);
        orderHandler.registerNotificationDeque(deque);
    }

    private ExecutorService startNoMoreOrdersMonitor(int periodShutDownMonitorSeconds, int pollingMaxRetries) {
        final ExecutorService noOrdersMonitorExecutor = Executors.newSingleThreadExecutor();
        final RetryBudget retryBudget = RetryBudget.of(pollingMaxRetries);
        CompletableFuture.runAsync(() -> {
            log.info("[SYSTEM] Monitoring delivery queue");
            while (retryBudget.hasMoreTokens()) {
                try {
                    if(!this.hasPendingDeliveryOrders()) {
                        retryBudget.spendRetryToken();
                    } else {
                        retryBudget.success();
                    }
                    Thread.sleep(periodShutDownMonitorSeconds);
                } catch (InterruptedException e) {
                    log.error("Thread interrupted: {}", e.getMessage(), e);
                }
            }
            this.deque.offer(new OutputEvent(EventType.NO_PENDING_ORDERS, KitchenClock.now()) {});
            log.info("[SYSTEM] no orders awaiting delivery");
        }, noOrdersMonitorExecutor);
        return noOrdersMonitorExecutor;
    }

    private boolean hasPendingDeliveryOrders() {
        MetricsProcessor.DeliveryMetricsSnapshot snapshot = this.metricsProcessor.snapshot();
        return Math.abs( snapshot.totalOrdersPrepared - snapshot.totalOrdersDelivered ) > 0;
    }

    private ExecutorService startOutputEventProcessors(int threadCount, final Deque<OutputEvent> deque) {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(threadCount);
        OutputEventHandler outputEventHandler = new OutputEventHandler(deque);
        scheduledExecutorService.scheduleAtFixedRate(outputEventHandler, 0, this.pollingTimeMillis, TimeUnit.MILLISECONDS);
        return scheduledExecutorService;
    }

    private CompletableFuture<Boolean> processOrder(final OrderReceivedEvent orderReceived) {
        DeliveryOrder order = orderReceived.order;
        final long reservationId = kitchenService.provideReservationId(order);
        CompletableFuture<Optional<Integer>> courierDispatch = CompletableFuture
                .supplyAsync(() -> courierService.dispatchRequest(order, reservationId));
        return courierDispatch.thenApply(courierId -> {
            if(courierId.isPresent()) {
                this.metricsProcessor.acceptOrderPrepareRequest();
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
        switch (outputEvent.type) {
            case ORDER_RECEIVED:
                this.metricsProcessor.acceptOrderReceived();
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
                log.info("[EVENT] order prepared: mealId[{}], orderId[{}] at {}", orderPreparedEvent.mealOrderId,
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
        log.info(String.format("[METRICS] Avg. Food Wait Time: [%.4f]ms, Avg Courier Wait Time [%.4f]ms",
                snapshot.avgFoodWaitTime, snapshot.avgFoodWaitTime));
    }

    @Override
    public void close() {
        if (!this.noMoreOrdersMonitor.isTerminated()) {
            log.info("[SYSTEM] queue processing shutting down, no orders remaining");
            this.noMoreOrdersMonitor.shutdown();
            this.eventHandlerExecutor.shutdown();
            this.courierService.shutdown();
            this.kitchenService.shutdown();
            if(null != this.context) {
                System.exit(SpringApplication.exit(this.context, () -> 0));
            }
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

    private class OutputEventHandler extends Thread {
        private final Deque<OutputEvent> deque;

        private OutputEventHandler(final Deque<OutputEvent> deque) {
            super("OutputEventHandlerThread");
            this.deque = deque;
        }

        @Override
        public void run() {
            try {
                OutputEvent take = deque.poll();
                if(take != null) {
                    dispatchOutputEvent(take);
                }
            } catch (Exception e) {
                log.error("Error thrown while executing OutputEventHandler: " + e.getMessage(), e);
            }
        }
    }
}
