package com.acabra.orderfullfilment.orderserver.courier.matcher;

import com.acabra.orderfullfilment.orderserver.event.*;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("DuplicatedCode")
@Component
@Slf4j
@ConditionalOnProperty(prefix = "orderserver", name = "strategy", havingValue = "fifo")
public class OrderCourierMatcherFIFOImpl implements OrderCourierMatcher {

    private final Deque<OrderPreparedEvent> mealsPrepared = new ConcurrentLinkedDeque<>();
    private final Deque<CourierArrivedEvent> couriersArrived = new ConcurrentLinkedDeque<>();
    private final AtomicReference<Deque<OutputEvent>> publicNotificationDeque = new AtomicReference<>();

    @Override
    public void registerNotificationDeque(Deque<OutputEvent> deque) {
        this.publicNotificationDeque.set(deque);
    }

    @Override
    public boolean acceptOrderPreparedEvent(OrderPreparedEvent orderPreparedEvent) {
        try {
            CourierArrivedEvent courierEvt = couriersArrived.poll();
            if (null != courierEvt) {
                publishedOrderPickedUpEvent(courierEvt, orderPreparedEvent);
            } else {
                mealsPrepared.offer(orderPreparedEvent);
            }
            return true;
        } catch (Exception e) {
            log.error("Unable to handle the mealPrepared event: {}", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public boolean acceptCourierArrivedEvent(CourierArrivedEvent courierArrivedEvent) {
        try {
            OrderPreparedEvent orderPreparedEvt = mealsPrepared.poll();
            if (null != orderPreparedEvt) {
                publishedOrderPickedUpEvent(courierArrivedEvent, orderPreparedEvt);
            } else {
                couriersArrived.offer(courierArrivedEvent);
            }
            return true;
        } catch (Exception e) {
            log.error("Unable to handle the mealPrepared event: {}", e.getMessage(), e);
        }
        return false;
    }

    private void publishedOrderPickedUpEvent(CourierArrivedEvent courierArrivedEvent,
                                             OrderPreparedEvent orderPreparedEvent) {
        try {
            long now = KitchenClock.now();
            OrderPickedUpEvent orderPickedUpEvent = OrderPickedUpEvent.of(now, courierArrivedEvent,
                    orderPreparedEvent.createdAt, orderPreparedEvent.mealOrderId);
            this.publicNotificationDeque.get().offer(orderPickedUpEvent);
        } catch (Exception e) {
            log.error("Unable to publish result to notification queue {}", e.getMessage(), e);
        }
    }

    //this event can be ignored as the matching takes place upon courier or meal arrival
    @Override
    public void processCourierDispatchedEvent(CourierDispatchedEvent courierDispatchedEvent) {}
}
