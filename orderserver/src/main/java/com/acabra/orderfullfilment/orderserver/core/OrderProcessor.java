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
import java.util.concurrent.atomic.LongAdder;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
public class OrderProcessor implements Closeable {
    /**
     * This is the main application service, it handles processing of orders
     * provides a public method close to finish threads consuming order events.
     */

    private static final OutputEvent NO_PENDING_ORDERS = new OutputEvent(EventType.NO_PENDING_ORDERS, KitchenClock.now()) {};

    private final CourierDispatchService courierService;
    private final KitchenService kitchenService;
    private final MetricsProcessor metricsProcessor;
    private final Deque<OutputEvent> deque;
    private final List<OutputEventHandler> eventConsumers;
    private final ExecutorService noMoreOrdersMonitor;
    private final LongAdder activeOrders = new LongAdder();

    public OrderProcessor(OrderServerConfig orderServerConfig,
                          CourierDispatchService courierService,
                          KitchenService kitchenService,
                          @Qualifier("order_handler") OutputEventPublisher orderHandler,
                          Deque<OutputEvent> deque) {
        this.metricsProcessor = new MetricsProcessor();
        this.deque = deque;
        this.eventConsumers = startOutputEventProcessors(orderServerConfig.getThreadCount(), deque);
        this.noMoreOrdersMonitor = startNoMoreOrdersMonitor(orderServerConfig.getPeriodShutDownMonitor());
        this.courierService = courierService;
        this.kitchenService = kitchenService;

        //register public notification queue
        this.courierService.registerNotificationDeque(deque);
        this.kitchenService.registerNotificationDeque(deque);
        orderHandler.registerNotificationDeque(deque);
    }

    private ExecutorService startNoMoreOrdersMonitor(int periodShutDownMonitorSeconds) {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            boolean allBlocked = this.eventConsumers.stream().allMatch(t -> Thread.State.BLOCKED == t.getState());
            long sum = activeOrders.sum();
            log.debug("[SYSTEM] {} orders awaiting delivery", sum);
            if(allBlocked && sum == 0) { // all received work completed
                this.deque.offer(NO_PENDING_ORDERS);
            }
        }, 1000L, periodShutDownMonitorSeconds, TimeUnit.SECONDS);
        return executorService;
    }

    private List<OutputEventHandler> startOutputEventProcessors(int threadCount,Deque<OutputEvent> deque) {
        List<OutputEventHandler> tasks = IntStream.range(0, threadCount)
                .mapToObj(i -> new OutputEventHandler(i, deque))
                .collect(Collectors.toList());
        tasks.forEach(CompletableFuture::runAsync);
        return tasks;
    }

    @Bean
    public static Deque<OutputEvent> buildNotificationDeque() {
        return new ConcurrentLinkedDeque<>();
    }

    private CompletableFuture<Boolean> processOrder(final OrderReceivedEvent orderReceived) {
        DeliveryOrder order = orderReceived.order;
        final Long reservationId = kitchenService.orderCookReservationId(order);
        CompletableFuture<Optional<Integer>> courierDispatch = CompletableFuture.supplyAsync(() -> courierService.dispatchRequest(order));
        return courierDispatch.thenApply(courierId -> {
            if(courierId.isPresent()) {
                kitchenService.prepareMeal(reservationId);
                return true;
            } else {
                activeOrders.decrement();
                log.info("No couriers available to deliver the order ... cancelling cooking reservation");
                kitchenService.cancelCookReservation(reservationId);
                return false;
            }
        });
    }

    private void dispatchOutputEvent(final OutputEvent outputEvent) {
        switch (outputEvent.type) {
            case ORDER_RECEIVED:
                activeOrders.increment();
                OrderReceivedEvent orderReceivedEvent = (OrderReceivedEvent) outputEvent;
                log.info("[EVENT] order received : {} at: {}" , orderReceivedEvent.order.id,
                        KitchenClock.formatted(orderReceivedEvent.createdAt));
                processOrder(orderReceivedEvent).join();
                break;
            case COURIER_DISPATCHED:
                CourierDispatchedEvent courierDispatchedEvent = (CourierDispatchedEvent) outputEvent;
                log.info("[EVENT] courier dispatched: id[{}]", courierDispatchedEvent.courierId);
                break;
            case ORDER_PREPARED:
                OrderPreparedEvent prepared = (OrderPreparedEvent) outputEvent;
                courierService.processOrderPrepared(prepared);
                break;
            case COURIER_ARRIVED:
                CourierArrivedEvent arrived = (CourierArrivedEvent) outputEvent;
                courierService.processCourierArrived(arrived);
                break;
            case ORDER_PICKED_UP:
                OrderPickedUpEvent pickedUp = (OrderPickedUpEvent) outputEvent;
                reportPickupMetrics(pickedUp);
                processOrderPickedUp(pickedUp);
                break;
            case ORDER_DELIVERED:
                activeOrders.decrement();
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
        log.info(String.format("[METRICS] Avg. Food Wait Time: [%.4f]ms, Avg Courier Wait Time [%.4f]ms",
                this.metricsProcessor.getAvgFoodWaitTime(), this.metricsProcessor.getAvgCourierWaitTime()));
    }

    @Override
    public void close() {
        if(this.noMoreOrdersMonitor.isTerminated()) {return;}

        reportAverageMetrics();
        this.noMoreOrdersMonitor.shutdownNow();
        OutputEvent sigPill = new OutputEvent(EventType.SHUT_DOWN_REQUEST, KitchenClock.now()) {};
        IntStream.range(0, eventConsumers.size())
            .forEach(i -> {
                try {
                     deque.offer(sigPill);
                } catch (Exception e) {
                    log.error("unable to signal termination to thread: {}", e.getMessage(), e);
                }
        });
    }

    private class OutputEventHandler extends Thread {
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
                    Thread.sleep(400L);
                }
            } catch (InterruptedException e) {
                log.error("Monitor interrupted thread failed!!" + e.getMessage(), e);
            }
            log.debug("{} released", this);
        }
    }
}
