package com.acabra.orderfullfilment.orderserver.courier.matcher;

import com.acabra.orderfullfilment.orderserver.event.*;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("DuplicatedCode")
@Component
@Slf4j
@ConditionalOnProperty(prefix = "orderserver", name = "strategy", havingValue = "matched")
public class OrderCourierMatcherMatchedImpl implements OrderCourierMatcher {

    private final AtomicReference<Queue<OutputEvent>> pubDeque = new AtomicReference<>();
    private final Map<Long, TimedEvent<OrderPreparedEvent>> ordersPrepared = new ConcurrentHashMap<>();
    private final Map<Integer, TimedEvent<CourierArrivedEvent>> couriersArrived = new ConcurrentHashMap<>();
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
                (courierId, timedEvent) -> {
                    if (null != timedEvent) {
                        long courierWaitTime = timedEvent.stop();
                        completeMatchingAndPublish(orderEvt, timedEvent.getEvent(), false, courierWaitTime);
                    } else {
                        ordersPrepared.put(orderEvt.kitchenReservationId, new TimedEvent<>(orderEvt));
                    }
                    return null;
                }
            );
            return true;
        } catch (Exception e) {
            log.error("[<<ERROR>>] Unable to accept the current event {}", e.getMessage());
        }
        return false;
    }

    synchronized private void completeMatchingAndPublish(OrderPreparedEvent orderEvt, CourierArrivedEvent courierEvt,
                                                         boolean completedByCourier, long waitTime) {
        reservationToCourierMap.remove(orderEvt.kitchenReservationId);
        courierToReservationMap.remove(courierEvt.courierId);
        long now = KitchenClock.now();
        publish(OrderPickedUpEvent.of(now, courierEvt, orderEvt, completedByCourier, waitTime));
    }

    @Override
    public boolean acceptCourierArrivedEvent(CourierArrivedEvent courierEvt) {
        try {
            ordersPrepared.compute(
                Objects.requireNonNull(courierToReservationMap.get(courierEvt.courierId),
                        "Unrecognized Courier Id: " + courierEvt.courierId),
                (orderId, timedEvent) -> {
                    if(null != timedEvent) {
                        long waitTime = timedEvent.stop();
                        completeMatchingAndPublish(timedEvent.getEvent(), courierEvt, true, waitTime);
                    } else {
                        couriersArrived.put(courierEvt.courierId, new TimedEvent<>(courierEvt));
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
    public void registerNotificationDeque(Queue<OutputEvent> deque) {
        this.pubDeque.set(deque);
    }

    @Override
    public Queue<OutputEvent> getPubDeque() {
        return this.pubDeque.get();
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
