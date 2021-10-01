package com.acabra.orderfullfilment.orderserver.courier.matcher;

import com.acabra.orderfullfilment.orderserver.event.*;
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

    private final Deque<OrderPreparedEvent> mealsPrepared = new ConcurrentLinkedDeque<>();
    private final Deque<CourierArrivedEvent> couriersArrived = new ConcurrentLinkedDeque<>();
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
    public boolean acceptOrderPreparedEvent(OrderPreparedEvent orderEvt) {
        try {
            CourierArrivedEvent courierEvt = couriersArrived.poll();
            if (null != courierEvt) {
                publish(OrderPickedUpEvent.of(courierEvt, orderEvt));
            } else {
                mealsPrepared.offer(orderEvt);
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
            OrderPreparedEvent orderEvt = mealsPrepared.poll();
            if (null != orderEvt) {
                publish(OrderPickedUpEvent.of(courierEvt, orderEvt));
            } else {
                couriersArrived.offer(courierEvt);
            }
            return true;
        } catch (Exception e) {
            log.error("Unable to handle the mealPrepared event: {}", e.getMessage(), e);
        }
        return false;
    }

    //this event can be ignored as the matching takes place upon courier or meal arrival
    @Override
    public void processCourierDispatchedEvent(CourierDispatchedEvent courierDispatchedEvent) {}
}
