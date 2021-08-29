package com.acabra.orderfullfilment.orderserver.courier.fifo;

import com.acabra.orderfullfilment.orderserver.courier.event.CourierReadyForPickupEvent;
import com.acabra.orderfullfilment.orderserver.courier.event.PickupCompletedEvent;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingDeque;
import java.util.function.Supplier;

@Slf4j
public class MealAwaitingPickupSupplier implements Supplier<PickupCompletedEvent> {

    private final BlockingDeque<CourierReadyForPickupEvent> queue;
    private final long readySince;

    public MealAwaitingPickupSupplier(BlockingDeque<CourierReadyForPickupEvent> queue, long readySince) {
        this.queue = queue;
        this.readySince = readySince;
    }

    @Override
    public PickupCompletedEvent get() {
        final CourierReadyForPickupEvent courier;
        try {
            courier = queue.take();
            long now = KitchenClock.now();
            PickupCompletedEvent pickupCompletedEvent = new PickupCompletedEvent(now, now - courier.arrivalTime, now - readySince, courier.courierId);
            return pickupCompletedEvent;
        } catch (InterruptedException e) {
            log.error("Failed to match a courier to deliver the order: {}", e.getMessage(), e);
            return null;
        }
    }

}
