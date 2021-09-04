package com.acabra.orderfullfilment.orderserver.courier;

import com.acabra.orderfullfilment.orderserver.courier.matcher.OrderCourierMatcher;
import com.acabra.orderfullfilment.orderserver.event.*;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
public class CourierServiceImpl implements CourierDispatchService {

    private final CourierFleet courierFleet;
    private final AtomicReference<Deque<OutputEvent>> publicNotificationQueue;
    private final OrderCourierMatcher orderCourierMatcher;

    @Autowired
    public CourierServiceImpl(CourierFleet courierFleet, OrderCourierMatcher orderCourierMatcher) {
        this.courierFleet = courierFleet;
        this.publicNotificationQueue = new AtomicReference<>();
        this.orderCourierMatcher = orderCourierMatcher;
    }

    @Override
    public Optional<Integer> dispatchRequest(DeliveryOrder order) {
        Integer courierId = this.courierFleet.dispatch(order).courierId;
        if(null != courierId) {
            publishCourierDispatchedNotification(order, courierId);
            return Optional.of(courierId);
        }
        return Optional.empty();
    }

    private void publishCourierDispatchedNotification(DeliveryOrder order, Integer courierId) {
        Deque<OutputEvent> deque = this.publicNotificationQueue.get();
        if(null != deque) {
            try {
                 deque.offer(CourierDispatchedEvent.of(KitchenClock.now(), order, courierId));
            } catch (Exception e) {
                log.error("Unable to publish courier dispatched event: {}", e.getMessage(), e);
            }
        }
    }

    @Override
    public CompletableFuture<Boolean> processOrderPrepared(OrderPreparedEvent orderPreparedEvent) {
        return CompletableFuture.supplyAsync(
                () -> orderCourierMatcher.acceptMealPreparedEvent(orderPreparedEvent));
    }

    @Override
    public CompletableFuture<Boolean> processCourierArrived(CourierArrivedEvent courierArrivedEvent) {
        return CompletableFuture.supplyAsync(() ->
                this.orderCourierMatcher.acceptCourierArrivedEvent(courierArrivedEvent));
    }

    @Override
    public CompletableFuture<Void> processOrderDelivered(OrderDeliveredEvent orderDeliveredEvent) {
        return CompletableFuture.runAsync(
                () -> courierFleet.release(orderDeliveredEvent.getCourierId()));
    }

    @Override
    public void registerNotificationDeque(Deque<OutputEvent> deque) {
        this.publicNotificationQueue.set(deque);
        this.orderCourierMatcher.registerNotificationDeque(deque);
        this.courierFleet.registerNotificationDeque(deque);
    }
}
