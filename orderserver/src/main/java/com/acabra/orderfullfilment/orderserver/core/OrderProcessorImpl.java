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

import java.util.Optional;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.IntStream;

@Service
@Slf4j
public class OrderProcessorImpl implements OrderProcessor {

    private final CourierDispatchService courierService;
    private final KitchenService kitchenService;
    private final MetricsProcessor metricsProcessor;

    public OrderProcessorImpl(OrderServerConfig orderServerConfig,
                              CourierDispatchService courierService,
                              KitchenService kitchenService,
                              @Qualifier("order_handler") OutputEventPublisher orderHandler, BlockingDeque<OutputEvent> deque) {
        startStartProcessorsList(orderServerConfig.getThreadCount(), deque);
        this.metricsProcessor = new MetricsProcessor();
        this.courierService = courierService;
        this.courierService.registerNotificationDeque(deque);
        this.kitchenService = kitchenService;
        this.kitchenService.registerNotificationDeque(deque);
        orderHandler.registerNotificationDeque(deque);
    }

    private void startStartProcessorsList(int threadCount, BlockingDeque<OutputEvent> deque) {
        IntStream.range(0,threadCount)
                .forEach(i-> {
                    OutputEventHandler outputEventHandler = new OutputEventHandler(i, deque);
                    CompletableFuture.runAsync(outputEventHandler::start);
                });
    }

    @Bean
    public static BlockingDeque<OutputEvent> buildNotificationDeque() {
        return new LinkedBlockingDeque<>();
    }

    @Override
    public CompletableFuture<Boolean> processOrder(final OrderReceivedEvent orderReceived) {
        log.info("[EVENT] order received : {} at: {}" , orderReceived.order.id, KitchenClock.formatted(orderReceived.createdAt));
        DeliveryOrder order = orderReceived.order;
        CompletableFuture<Long> kitchenReservation = CompletableFuture.supplyAsync(() -> kitchenService.orderCookReservationId(order));
        CompletableFuture<Optional<Integer>> courierDispatch = kitchenReservation.thenApply(a -> courierService.dispatchRequest(order));
        return courierDispatch.thenCombineAsync(kitchenReservation, (courierId, reservationId) -> {
            if(courierId.isPresent()) {
                log.info("[EVENT] courier dispatched: id[{}]", courierId.get());
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
            case ORDER_PREPARED:
                OrderPreparedEvent prepared = (OrderPreparedEvent) outputEvent;
                courierService.processMealReady(prepared);
                break;
            case ORDER_RECEIVED:
                OrderReceivedEvent orderReceivedEvent = (OrderReceivedEvent) outputEvent;
                processOrder(orderReceivedEvent);
                break;
            case ORDER_PICKED_UP:
                OrderPickedUpEvent pickedUp = (OrderPickedUpEvent) outputEvent;
                processOrderPickedUp(pickedUp);
                break;
            case ORDER_DELIVERED:
                OrderDeliveredEvent orderDeliveredEvent = (OrderDeliveredEvent) outputEvent;
                log.info("[EVENT] order delivered: orderId[{}] by courierId[{}] at {}",
                    orderDeliveredEvent.mealOrderId, orderDeliveredEvent.courierId,
                        KitchenClock.formatted(orderDeliveredEvent.createdAt));
                break;
            default:
                throw new UnsupportedOperationException("Event not recognized by the system" + outputEvent.type);
        }
    }

    private void processOrderPickedUp(OrderPickedUpEvent orderPickedUpEvent) {
        recordMetrics(orderPickedUpEvent);
        //emulate instant delivery TODO: [TRACKER-TICKET-SYSTEM-1235 implement real delivery]
        this.dispatchOutputEvent(OrderDeliveredEvent.of(orderPickedUpEvent));
    }

    private void recordMetrics(OrderPickedUpEvent orderPickedUpEvent) {
        this.metricsProcessor.acceptFoodWaitTime(orderPickedUpEvent.foodWaitTime);
        log.info("[METRICS] Food wait time: {}ms, order {}", orderPickedUpEvent.foodWaitTime,
                orderPickedUpEvent.mealOrderId);
        this.metricsProcessor.acceptCourierWaitTime(orderPickedUpEvent.courierWaitTime);
        log.info("[METRICS] Courier wait time {}ms on orderId:[{}]", orderPickedUpEvent.courierWaitTime,
                orderPickedUpEvent.mealOrderId);
    }

    private class OutputEventHandler extends Thread {
        private final BlockingDeque<OutputEvent> deque;
        volatile boolean end = false;

        private OutputEventHandler(final int id, final BlockingDeque<OutputEvent> deque) {
            super("OutputEventHandlerThread " + id);
            this.deque = deque;
        }

        @Override
        public void start() {
            log.info("{} started", this);
            try {
                while(!end) {
                    OutputEvent take = deque.take();
                    CompletableFuture.runAsync(() -> dispatchOutputEvent(take));
                }
            } catch (InterruptedException e) {
                log.error("Monitor interrupted thread failed!!" + e.getMessage(), e);
            }
        }
    }
}
