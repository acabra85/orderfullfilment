package com.acabra.orderfullfilment.orderserver.core;

import com.acabra.orderfullfilment.orderserver.config.OrderServerConfig;
import com.acabra.orderfullfilment.orderserver.courier.CourierDispatchService;
import com.acabra.orderfullfilment.orderserver.event.*;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenService;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
public class OrderProcessor implements Closeable {
    /**
     * This is the main application service, it handles processing of orders
     * provides a public method close to finish threads consuming order events.
     */

    private final CourierDispatchService courierService;
    private final KitchenService kitchenService;
    private final MetricsProcessor metricsProcessor;
    private final Deque<OutputEvent> deque;
    private final List<OutputEventHandler> eventConsumers;
    private final long pollingTimeMillis;
    private final ExecutorService noMoreOrdersMonitor;

    public OrderProcessor(OrderServerConfig orderServerConfig,
                          CourierDispatchService courierService,
                          KitchenService kitchenService,
                          @Qualifier("order_handler") OutputEventPublisher orderHandler,
                          @Qualifier("notification_deque") Deque<OutputEvent> deque) {
        this.metricsProcessor = new MetricsProcessor();
        this.deque = deque;
        this.pollingTimeMillis = orderServerConfig.getPollingTimeMillis();
        this.eventConsumers = startOutputEventProcessors(orderServerConfig.getThreadCount(), deque);
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
            log.debug("[SYSTEM] Monitoring delivery queue");
            while (retryBudget.hasMoreTokens()) {
                try {
                    if(deque.isEmpty()) {
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
            log.debug("[SYSTEM] no orders awaiting delivery");
        }, noOrdersMonitorExecutor);
        return noOrdersMonitorExecutor;
    }

    private List<OutputEventHandler> startOutputEventProcessors(int threadCount, Deque<OutputEvent> deque) {
        List<OutputEventHandler> tasks = IntStream.range(0, threadCount)
                .mapToObj(i -> new OutputEventHandler(i, deque))
                .collect(Collectors.toList());
        tasks.forEach(CompletableFuture::runAsync);
        return tasks;
    }

    private CompletableFuture<Boolean> processOrder(final OrderReceivedEvent orderReceived) {
        DeliveryOrder order = orderReceived.order;
        final long reservationId = kitchenService.orderCookReservationId(order);
        CompletableFuture<Optional<Integer>> courierDispatch = CompletableFuture
                .supplyAsync(() -> courierService.dispatchRequest(order));
        return courierDispatch.thenApply(courierId -> {
            if(courierId.isPresent()) {
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
                log.info("[EVENT] order received : {} at: {}" , orderReceivedEvent.order.id,
                        KitchenClock.formatted(orderReceivedEvent.createdAt));
                processOrder(orderReceivedEvent).join();
                break;
            case COURIER_DISPATCHED:
                CourierDispatchedEvent courierDispatchedEvent = (CourierDispatchedEvent) outputEvent;
                log.info("[EVENT] courier dispatched: id[{}] estimated travel time [{}]ms",
                        courierDispatchedEvent.courierId, courierDispatchedEvent.estimatedTravelTime);
                break;
            case ORDER_PREPARED:
                OrderPreparedEvent orderPreparedEvent = (OrderPreparedEvent) outputEvent;
                log.info("[EVENT] order prepared: mealId{}, orderId[{}] at {}", orderPreparedEvent.mealOrderId,
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
        log.info("[METRICS] Food wait time: {}ms, order {}", orderPickedUpEvent.foodWaitTime,
                orderPickedUpEvent.mealOrderId);
        this.metricsProcessor.acceptCourierWaitTime(orderPickedUpEvent.courierWaitTime);
        log.info("[METRICS] Courier wait time {}ms on orderId:[{}]", orderPickedUpEvent.courierWaitTime,
                orderPickedUpEvent.mealOrderId);
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
            OutputEvent sigPill = new OutputEvent(EventType.SHUT_DOWN_REQUEST, KitchenClock.now()){};
            IntStream.range(0, eventConsumers.size())
                .forEach(i -> {
                    try {
                        deque.offer(sigPill);
                    } catch (Exception e) {
                        log.error("unable to signal termination to thread: {}", e.getMessage(), e);
                    }
                });
        }
    }

    @Bean("notification_deque")
    public static Deque<OutputEvent> buildNotificationDeque() {
        return new ConcurrentLinkedDeque<>();
    }

    public MetricsProcessor.DeliveryMetricsSnapshot getMetricsSnapshot() {
        return this.metricsProcessor.snapshot();
    }

    private class OutputEventHandler extends Thread {
        private final long POLLING_TIME_MILLIS = OrderProcessor.this.pollingTimeMillis;
        private final Deque<OutputEvent> deque;
        private volatile boolean finish;

        private OutputEventHandler(final int id, final Deque<OutputEvent> deque) {
            super("OutputEventHandlerThread " + id);
            this.deque = deque;
        }

        @Override
        public void run() {
            log.debug("{} started", this);
            try {
                while(!finish) {
                    OutputEvent take = deque.poll();
                    if(take != null) {
                        if(take.type == EventType.SHUT_DOWN_REQUEST) {
                            finish = true;
                            break;
                        }
                        dispatchOutputEvent(take);
                    }
                    Thread.sleep(POLLING_TIME_MILLIS);
                }
            } catch (InterruptedException e) {
                log.error("Monitor interrupted thread failed!!" + e.getMessage(), e);
            }
            log.debug("{} released", this);
        }
    }
}
