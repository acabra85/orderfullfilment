package com.acabra.orderfullfilment.orderserver.courier.fifo;

import com.acabra.orderfullfilment.orderserver.event.CourierArrivedEvent;
import com.acabra.orderfullfilment.orderserver.event.OrderPickedUpEvent;
import com.acabra.orderfullfilment.orderserver.event.OrderPreparedEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingDeque;
import java.util.function.Supplier;

@Slf4j
public class MealAwaitingPickupSupplier implements Supplier<OrderPickedUpEvent> {

    private final BlockingDeque<OutputEvent> queue;
    private final long orderReadySince;
    private final long mealOrderId;

    private MealAwaitingPickupSupplier(BlockingDeque<OutputEvent> queue, long orderReadySince, long mealOrderId) {
        this.queue = queue;
        this.orderReadySince = orderReadySince;
        this.mealOrderId = mealOrderId;
    }

    public static MealAwaitingPickupSupplier of(BlockingDeque<OutputEvent> deque, OrderPreparedEvent orderPreparedEvent) {
        return new MealAwaitingPickupSupplier(deque, orderPreparedEvent.createdAt, orderPreparedEvent.mealOrderId);
    }

    @Override
    public OrderPickedUpEvent get() {
        try {
            CourierArrivedEvent courierArrivedEvent = (CourierArrivedEvent) queue.take();
            return OrderPickedUpEvent.of(KitchenClock.now(), courierArrivedEvent,
                    orderReadySince, mealOrderId);
        } catch (InterruptedException e) {
            log.error("Failed to match a courier to deliver the order: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error exception caught: {}", e.getMessage(), e);
        }
        return null;
    }

}
