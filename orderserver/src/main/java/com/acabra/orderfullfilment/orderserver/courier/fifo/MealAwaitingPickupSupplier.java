package com.acabra.orderfullfilment.orderserver.courier.fifo;

import com.acabra.orderfullfilment.orderserver.courier.event.PickupCompletedEvent;
import com.acabra.orderfullfilment.orderserver.courier.event.CourierReadyForPickupEvent;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import com.acabra.orderfullfilment.orderserver.kitchen.event.MealReadyForPickupEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;
import java.util.function.Supplier;

@Slf4j
public class MealAwaitingPickupSupplier implements Supplier<PickupCompletedEvent> {

    private final LinkedBlockingDeque<CourierReadyForPickupEvent> queue;
    private final MealReadyForPickupEvent mealReadyForPickup;

    public MealAwaitingPickupSupplier(LinkedBlockingDeque<CourierReadyForPickupEvent> queue,
                                      MealReadyForPickupEvent courierReadyForPickupEvent) {
        this.queue = queue;
        this.mealReadyForPickup = courierReadyForPickupEvent;
    }

    @Override
    public PickupCompletedEvent get() {
        try {
            while (true) {
                final CourierReadyForPickupEvent courier = queue.poll();
                if (null != courier) {
                    long now = KitchenClock.now();
                    return new PickupCompletedEvent(now, now - courier.arrivalTime,
                            now - mealReadyForPickup.readySince, courier.courierId);
                }
                awaitBeforeRetry();
            }
        } catch (InterruptedException ie) {
            log.error("CourierAwaitingAnyOrderTask interrupted: ", ie);
            Thread.currentThread().interrupt();
        } catch (Exception exception) {
            log.error("CourierAwaitingAnyOrderTask thrown while run loop: ", exception);
            exception.printStackTrace();
        }
        return null;
    }

    private void awaitBeforeRetry() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(1);
        CompletableFuture.runAsync(countDownLatch::countDown,
                CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS));
        countDownLatch.await();
    }
}
