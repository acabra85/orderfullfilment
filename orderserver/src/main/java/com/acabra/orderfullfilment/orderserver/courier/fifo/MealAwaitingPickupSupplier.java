package com.acabra.orderfullfilment.orderserver.courier.fifo;

import com.acabra.orderfullfilment.orderserver.courier.event.CourierReadyForPickupEvent;
import com.acabra.orderfullfilment.orderserver.courier.event.PickupCompletedEvent;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Supplier;

@Slf4j
public class MealAwaitingPickupSupplier implements Supplier<PickupCompletedEvent> {

    private final LinkedBlockingDeque<CourierReadyForPickupEvent> queue;
    private final long readySince;

    public MealAwaitingPickupSupplier(LinkedBlockingDeque<CourierReadyForPickupEvent> queue, long readySince) {
        this.queue = queue;
        this.readySince = readySince;
    }

    @Override
    public PickupCompletedEvent get() {
        final CourierReadyForPickupEvent courier = queue.poll();
        long now = KitchenClock.now();
        return new PickupCompletedEvent(now, now - courier.arrivalTime, now - readySince, courier.courierId);
    }
}
