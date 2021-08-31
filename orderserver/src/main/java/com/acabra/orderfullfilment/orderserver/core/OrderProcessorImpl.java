package com.acabra.orderfullfilment.orderserver.core;

import com.acabra.orderfullfilment.orderserver.config.OrderServerConfig;
import com.acabra.orderfullfilment.orderserver.courier.CourierDispatchService;
import com.acabra.orderfullfilment.orderserver.event.*;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenService;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
public class OrderProcessorImpl implements OrderProcessor {

    private final CourierDispatchService courierService;
    private final KitchenService kitchenService;
    private final MetricsProcessor metricsProcessor;
    private final BlockingDeque<OutputEvent> notificationDeque;

    public OrderProcessorImpl(OrderServerConfig orderServerConfig,
                              CourierDispatchService courierService,
                              KitchenService kitchenService,
                              OrderRequestHandler orderHandler) {
        final BlockingDeque<OutputEvent> deque = new LinkedBlockingDeque<>();
        startStartProcessorsList(orderServerConfig.getThreadCount(), deque);
        this.notificationDeque = deque;
        this.metricsProcessor = new MetricsProcessor();
        this.courierService = courierService;
        this.courierService.registerNotificationDeque(deque);
        this.kitchenService = kitchenService;
        this.kitchenService.registerMealNotificationReadyQueue(deque);
        orderHandler.registerNotificationDeque(deque);
    }

    private List<OutputEventHandler> startStartProcessorsList(int threadCount, BlockingDeque<OutputEvent> deque) {
        return IntStream.range(0,threadCount)
                .mapToObj(i-> {
                    OutputEventHandler outputEventHandler = new OutputEventHandler(i, deque);
                    CompletableFuture.runAsync(outputEventHandler::start);
                    return outputEventHandler;
                })
                .collect(Collectors.toList());
    }

    @Override
    public void processOrder(final OrderReceivedEvent orderReceived) {
        log.info("[EVENT] order received : {} at: {}" , orderReceived.order.id, KitchenClock.formatted(orderReceived.createdAt));
        DeliveryOrder order = orderReceived.order;
        CompletableFuture<Long> kitchenReservation = CompletableFuture.supplyAsync(() -> kitchenService.orderCookReservationId(order));
        CompletableFuture<Optional<Integer>> courierDispatch = kitchenReservation.thenApply(a -> courierService.dispatchRequest(order));

        courierDispatch.thenAcceptBoth(kitchenReservation, (courierId, reservationId) -> {
            if(courierId.isPresent()) {
                log.info("[EVENT] courier dispatched: id[{}]", courierId.get());
                kitchenService.prepareMeal(reservationId);
            } else {
                log.info("No couriers available to deliver the order ... cancelling cooking reservation");
                kitchenService.cancelCookReservation(reservationId);
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

        //note we are publishing a delivery note with the exact same time as the pickup time (instant delivery)
        publishDeliveryEvent(OrderDeliveredEvent.of(orderPickedUpEvent));
    }

    private void publishDeliveryEvent(OrderDeliveredEvent orderDeliveredEvent) {
        try {
            this.notificationDeque.put(orderDeliveredEvent);
        } catch (InterruptedException e) {
            log.error("Failure to publish the delivery notification event ");
        }
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

        public OutputEventHandler(final int id, final BlockingDeque<OutputEvent> deque) {
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
