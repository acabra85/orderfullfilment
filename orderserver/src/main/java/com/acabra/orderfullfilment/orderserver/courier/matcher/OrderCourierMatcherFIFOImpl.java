package com.acabra.orderfullfilment.orderserver.courier.matcher;

import com.acabra.orderfullfilment.orderserver.event.CourierArrivedEvent;
import com.acabra.orderfullfilment.orderserver.event.OrderPickedUpEvent;
import com.acabra.orderfullfilment.orderserver.event.OrderPreparedEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;

@SuppressWarnings("DuplicatedCode")
@Component
@Slf4j
@ConditionalOnProperty(prefix = "orderserver", name = "strategy", havingValue = "fifo")
public class OrderCourierMatcherFIFOImpl implements OrderCourierMatcher {

    private final BlockingDeque<OrderPreparedEvent> mealsPrepared = new LinkedBlockingDeque<>();
    private final BlockingDeque<CourierArrivedEvent> couriersArrived = new LinkedBlockingDeque<>();
    private final AtomicReference<BlockingDeque<OutputEvent>> publicNotificationDeque = new AtomicReference<>();

    @Override
    public void registerNotificationDeque(BlockingDeque<OutputEvent> deque) {
        this.publicNotificationDeque.set(deque);
    }

    @Override
    public boolean acceptMealPreparedEvent(OrderPreparedEvent orderPreparedEvt) {
        try {
            CourierArrivedEvent courierEvt = couriersArrived.poll();
            if (null != courierEvt) {
                publishedOrderPickedUpEvent(courierEvt, orderPreparedEvt);
            } else {
                mealsPrepared.offer(orderPreparedEvt);
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
            log.info("[EVENT] order picked up: orderId[{}] courierId[{}] at {}",
                    orderPreparedEvent.deliveryOrderId, orderPickedUpEvent.courierId,
                    KitchenClock.formatted(orderPickedUpEvent.createdAt));
            this.publicNotificationDeque.get().offer(orderPickedUpEvent);
        } catch (Exception e) {
            log.error("Unable to publish result to notification queue {}", e.getMessage(), e);
        }
    }
}
