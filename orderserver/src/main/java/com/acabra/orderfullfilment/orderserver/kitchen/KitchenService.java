package com.acabra.orderfullfilment.orderserver.kitchen;

import com.acabra.orderfullfilment.orderserver.kitchen.event.MealReadyForPickupEvent;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class KitchenService {
    final AtomicLong cookingOrderId;
    private final ScheduledExecutorService scheduleExecutor;
    private final HashMap<DeliveryOrder, Long> hashMap;

    public KitchenService() {
        this.scheduleExecutor = Executors.newSingleThreadScheduledExecutor();
        cookingOrderId = new AtomicLong();
        hashMap = new HashMap<>();
    }

    synchronized public void cookOrderRequest(DeliveryOrder order, long mealOrderId) {
        if(hashMap.containsKey(order) || mealOrderId >= cookingOrderId.get()) {
            RequestCookEvent requestCookEvent = RequestCookEvent.ofReadyForPickUpMeal(mealOrderId);
            log.info("Kitchen started to prepare meal : {} for order: {}", mealOrderId, order);
            CompletableFuture.runAsync(() ->
                            scheduleExecutor.schedule(requestCookEvent, order.prepTime, TimeUnit.MILLISECONDS)
                    ).join();
            return;
        }
        String template = "Unable to find the pre-order id[%d] for the given order: %s";
        throw new NoSuchElementException(String.format(template, mealOrderId, order.toString()));
    }

    synchronized public long preRequestNextMealId(DeliveryOrder order) {
        long mealOrderId = nextOrderId();
        hashMap.put(order, mealOrderId);
        log.info("Id requested for meal order: {} given: {}", order, mealOrderId);
        return mealOrderId;
    }

    private long nextOrderId() {
        return cookingOrderId.getAndIncrement();
    }

    private static class RequestCookEvent implements Runnable {
        private final long mealOrderId;

        public RequestCookEvent(long mealOrderId) {
            this.mealOrderId = mealOrderId;
        }

        public static RequestCookEvent ofReadyForPickUpMeal(long mealOrderId) {
            return new RequestCookEvent(mealOrderId);
        }

        @Override
        public void run() {
            MealReadyForPickupEvent readyForPickup = MealReadyForPickupEvent.of(this.mealOrderId, KitchenClock.now());
            log.info("Meal {} Ready for pickup ... since: {} ", readyForPickup.mealOrderId,
                    KitchenClock.formatted(readyForPickup.readySince));
        }
    }
}
