package com.acabra.orderfullfilment.orderserver.courier.matcher;

import com.acabra.orderfullfilment.orderserver.event.*;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Deque;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("DuplicatedCode")
@Component
@Slf4j
@ConditionalOnProperty(prefix = "orderserver", name = "strategy", havingValue = "matched")
public class OrderCourierMatcherMatchedImpl implements OrderCourierMatcher {

    private final AtomicReference<Deque<OutputEvent>> pubDeque = new AtomicReference<>();
    private final Map<Long, OrderPreparedEvent> ordersPrepared = new ConcurrentHashMap<>();
    private final Map<Integer, CourierArrivedEvent> couriersArrived = new ConcurrentHashMap<>();
    private final Map<Integer, Long> courierToReservationMap = new ConcurrentHashMap<>();
    private final Map<Long, Integer> reservationToCourierMap = new ConcurrentHashMap<>();

    public OrderCourierMatcherMatchedImpl() {
        log.info("[SYSTEM] Initialized the server using the dispatch MATCHED strategy");
    }

    @Override
    public boolean acceptOrderPreparedEvent(OrderPreparedEvent orderEvt) {
        try {
            couriersArrived.compute(
                Objects.requireNonNull(reservationToCourierMap.get(orderEvt.kitchenReservationId),
                        "Unrecognized Reservation Id: " + orderEvt.kitchenReservationId),
                (courierId, courierArrivedEvent) -> {
                    if (null != courierArrivedEvent) {
                        completeMatchingAndPublish(orderEvt, courierArrivedEvent);
                    } else {
                        ordersPrepared.put(orderEvt.kitchenReservationId, orderEvt);
                    }
                    return null;
                }
            );
            return true;
        } catch (Exception e) {
            log.error("Unable to accept the current event {}", e.getMessage());
        }
        return false;
    }

    synchronized private void completeMatchingAndPublish(OrderPreparedEvent orderEvt, CourierArrivedEvent courierEvt) {
        reservationToCourierMap.remove(orderEvt.kitchenReservationId);
        courierToReservationMap.remove(courierEvt.courierId);
        publish(OrderPickedUpEvent.of(KitchenClock.now(), courierEvt,orderEvt.createdAt, orderEvt.kitchenReservationId));
    }

    @Override
    public boolean acceptCourierArrivedEvent(CourierArrivedEvent courierEvt) {
        try {
            ordersPrepared.compute(
                Objects.requireNonNull(courierToReservationMap.get(courierEvt.courierId),
                        "Unrecognized Courier Id: " + courierEvt.courierId),
                (orderId, orderPreparedEvent) -> {
                    if(null != orderPreparedEvent) {
                        completeMatchingAndPublish(orderPreparedEvent, courierEvt);
                    } else {
                        couriersArrived.put(courierEvt.courierId, courierEvt);
                    }
                    return null;
                }
            );
            return true;
        } catch (Exception e) {
            log.error("Unable to accept the current event {}", e.getMessage());
        }
        return false;
    }

    @Override
    public void registerNotificationDeque(Deque<OutputEvent> deque) {
        this.pubDeque.set(deque);
    }

    @Override
    public AtomicReference<Deque<OutputEvent>> getPubDeque() {
        return this.pubDeque;
    }

    @Override
    public Logger log() {
        return log;
    }

    @Override
    synchronized public void processCourierDispatchedEvent(CourierDispatchedEvent courierEvt) {
        courierToReservationMap.put(courierEvt.courierId, courierEvt.kitchenReservationId);
        reservationToCourierMap.put(courierEvt.kitchenReservationId, courierEvt.courierId);
    }
}
