package com.acabra.orderfullfilment.orderserver.courier.matcher;

import com.acabra.orderfullfilment.orderserver.event.*;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("DuplicatedCode")
@Component
@Slf4j
@ConditionalOnProperty(prefix = "orderserver", name = "strategy", havingValue = "fifo")
public class OrderCourierMatcherFIFOImpl implements OrderCourierMatcher {

    private final Deque<TimedEvent<OrderPreparedEvent>> mealsPrepared = new ConcurrentLinkedDeque<>();
    private final Deque<TimedEvent<CourierArrivedEvent>> couriersArrived = new ConcurrentLinkedDeque<>();
    private final AtomicReference<Queue<OutputEvent>> pubDeque = new AtomicReference<>();

    public OrderCourierMatcherFIFOImpl() {
        log.info("[SYSTEM] Initialized the server using the dispatch FIFO strategy");
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
    public boolean acceptOrderPreparedEvent(OrderPreparedEvent orderPreparedEvent) {
        try {
            TimedEvent<CourierArrivedEvent> courierEvt = couriersArrived.poll();
            if (null != courierEvt) {
                long waitTime = courierEvt.stop();
                publishedOrderPickedUpEvent(courierEvt.getEvent(), orderPreparedEvent, false, waitTime);
            } else {
                mealsPrepared.offer(new TimedEvent<>(orderPreparedEvent));
            }
            return true;
        } catch (Exception e) {
            log.error("Unable to handle the mealPrepared event: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean acceptCourierArrivedEvent(CourierArrivedEvent courierEvt) {
        try {
            TimedEvent<OrderPreparedEvent> timedEvent = mealsPrepared.poll();
            if (null != timedEvent) {
                long waitTime = timedEvent.stop();
                publishedOrderPickedUpEvent(courierEvt, timedEvent.getEvent(), true, waitTime);
            } else {
                couriersArrived.offer(new TimedEvent<>(courierEvt));
            }
            return true;
        } catch (Exception e) {
            log.error("Unable to handle the mealPrepared event: {}", e.getMessage(), e);
        }
        return false;
    }

    private void publishedOrderPickedUpEvent(CourierArrivedEvent courierEvt, OrderPreparedEvent orderEvt,
                                             boolean courierCompleted, long waitTime) {
        publish(OrderPickedUpEvent.of(KitchenClock.now(), courierEvt, orderEvt, courierCompleted, waitTime));
    }

    //this event can be ignored as the matching takes place upon courier or meal arrival
    @Override
    public void processCourierDispatchedEvent(CourierDispatchedEvent courierDispatchedEvent) {}
}
