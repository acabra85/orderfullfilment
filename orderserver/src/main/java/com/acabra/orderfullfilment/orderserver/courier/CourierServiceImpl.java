package com.acabra.orderfullfilment.orderserver.courier;

import com.acabra.orderfullfilment.orderserver.courier.matcher.OrderCourierMatcher;
import com.acabra.orderfullfilment.orderserver.courier.model.DispatchResult;
import com.acabra.orderfullfilment.orderserver.event.*;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
public class CourierServiceImpl implements CourierDispatchService {

    private final CourierFleet courierFleet;
    private final AtomicReference<Deque<OutputEvent>> pubDeque;
    private final OrderCourierMatcher orderCourierMatcher;

    @Autowired
    public CourierServiceImpl(CourierFleet courierFleet, OrderCourierMatcher orderCourierMatcher) {
        this.courierFleet = courierFleet;
        this.pubDeque = new AtomicReference<>();
        this.orderCourierMatcher = orderCourierMatcher;
    }

    @Override
    public Optional<Integer> dispatchRequest(DeliveryOrder order, long kitchenReservationId) {
        DispatchResult dispatchResult = this.courierFleet.dispatch(order);
        Integer courierId = dispatchResult.courierId;
        if(null != courierId) {
            OutputEvent event = CourierDispatchedEvent.of(KitchenClock.now(), order, courierId, kitchenReservationId,
                    dispatchResult.estimatedTravelTime);
            publish(event);
            return Optional.of(courierId);
        }
        return Optional.empty();
    }

    @Override
    public boolean processOrderPrepared(OrderPreparedEvent orderPreparedEvent) {
        return orderCourierMatcher.acceptOrderPreparedEvent(orderPreparedEvent);
    }

    @Override
    public boolean processCourierArrived(CourierArrivedEvent courierArrivedEvent) {
        return this.orderCourierMatcher.acceptCourierArrivedEvent(courierArrivedEvent);
    }

    @Override
    public void processOrderDelivered(OrderDeliveredEvent orderDeliveredEvent) {
        courierFleet.release(orderDeliveredEvent.getCourierId());
    }

    @Override
    public void registerNotificationDeque(Deque<OutputEvent> deque) {
        this.pubDeque.set(deque);
        this.orderCourierMatcher.registerNotificationDeque(deque);
        this.courierFleet.registerNotificationDeque(deque);
    }

    @Override
    public Deque<OutputEvent> getPubDeque() {
        return pubDeque.get();
    }

    @Override
    public Logger log() {
        return log;
    }

    @Override
    public void processCourierDispatchedEvent(CourierDispatchedEvent courierDispatchedEvent) {
        this.orderCourierMatcher.processCourierDispatchedEvent(courierDispatchedEvent);
    }

    @Override
    public void shutdown() {
        log.info("Courier service shutdown");
    }
}
