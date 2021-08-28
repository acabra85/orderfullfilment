package com.acabra.orderfullfilment.orderserver.kitchen;

import com.acabra.orderfullfilment.orderserver.courier.CourierDispatchService;
import com.acabra.orderfullfilment.orderserver.kitchen.event.MealReadyForPickupEvent;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class KitchenServiceImpl implements KitchenService {
    final AtomicLong cookingOrderId;
    private final ConcurrentHashMap<Long, DeliveryOrder> internalIdToOrder;
    private final CourierDispatchService courierDispatchService;

    public KitchenServiceImpl(CourierDispatchService courierDispatchService) {
        cookingOrderId = new AtomicLong();
        internalIdToOrder = new ConcurrentHashMap<>();
        this.courierDispatchService = courierDispatchService;
    }

    private long nextOrderId() {
        return cookingOrderId.getAndIncrement();
    }

    @Override
    public void prepareMeal(long mealOrderId) {
        DeliveryOrder order = internalIdToOrder.get(mealOrderId);
        if(order != null) {
            RequestCookEvent requestCookEvent = new RequestCookEvent(mealOrderId, order.id);
            log.debug("Kitchen started to prepare meal : {} for order: {}", order.name, order.id);
            CompletableFuture.runAsync(requestCookEvent,
                    CompletableFuture.delayedExecutor(order.prepTime, TimeUnit.MILLISECONDS));
            return;
        }
        String template = "Unable to find the cookReservationId id[%d] for the given order: %d";
        throw new NoSuchElementException(String.format(template, mealOrderId, order.id));
    }

    @Override
    public long orderCookReservationId(DeliveryOrder order) {
        long mealOrderId = nextOrderId();
        internalIdToOrder.put(mealOrderId, order);
        log.debug("Id requested for meal order: {} given: {}", order.id, mealOrderId);
        return mealOrderId;
    }

    @Override
    public void cancelCookReservation(long mealReservationId) {
        internalIdToOrder.remove(mealReservationId);
    }

    private class RequestCookEvent implements Runnable {
        private final long mealOrderId;
        private final String deliveryOrderId;

        public RequestCookEvent(long mealReservationId, String deliveryOrderId) {
            this.mealOrderId = mealReservationId;
            this.deliveryOrderId = deliveryOrderId;
        }

        @Override
        public void run() {
            MealReadyForPickupEvent readyForPickup = MealReadyForPickupEvent.of(this.mealOrderId,
                    this.deliveryOrderId,
                    KitchenClock.now());
            log.info("[EVENT] Order prepared: id[{}] Ready for pickup at: {}", deliveryOrderId,
                    KitchenClock.formatted(readyForPickup.readySince));
            KitchenServiceImpl.this.courierDispatchService.processMealReady(readyForPickup);
        }
    }
}
