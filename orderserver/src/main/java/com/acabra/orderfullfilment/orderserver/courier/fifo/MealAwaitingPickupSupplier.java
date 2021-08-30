package com.acabra.orderfullfilment.orderserver.courier.fifo;

import com.acabra.orderfullfilment.orderserver.event.CourierArrivedEvent;
import com.acabra.orderfullfilment.orderserver.event.OrderPickedUpEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingDeque;
import java.util.function.Supplier;

@Slf4j
public class MealAwaitingPickupSupplier implements Supplier<OrderPickedUpEvent> {

    private final BlockingDeque<OutputEvent> queue;
    private final long readySince;

    public MealAwaitingPickupSupplier(BlockingDeque<OutputEvent> queue, long readySince) {
        this.queue = queue;
        this.readySince = readySince;
    }

    @Override
    public OrderPickedUpEvent get() {
        try {
            CourierArrivedEvent courier = (CourierArrivedEvent) queue.take();
            long now = KitchenClock.now();
            OrderPickedUpEvent ev = new OrderPickedUpEvent(now, now - courier.createdAt, now - readySince, courier.courierId);
            return ev;
        } catch (InterruptedException e) {
            log.error("Failed to match a courier to deliver the order: {}", e.getMessage(), e);
        } catch (Exception e) {
            log.error("Error exception caught: {}", e.getMessage(), e);
        }
        throw new RuntimeException("Unable to complete the OrderPickupEvent");
    }

}
