package com.acabra.orderfullfilment.orderserver.kitchen;

import com.acabra.orderfullfilment.orderserver.event.OrderPreparedEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

@Service
@Slf4j
public class KitchenServiceImpl implements KitchenService {
    private final AtomicLong cookingOrderId;
    private final ConcurrentHashMap<Long, DeliveryOrder> internalIdToOrder;
    private final AtomicReference<Deque<OutputEvent>> publicNotificationDeque;
    private final LongAdder mealsUnderPreparation;

    public KitchenServiceImpl() {
        this.cookingOrderId = new AtomicLong();
        this.internalIdToOrder = new ConcurrentHashMap<>();
        this.publicNotificationDeque = new AtomicReference<>();
        this.mealsUnderPreparation = new LongAdder();
    }

    @Override
    public CompletableFuture<Boolean> prepareMeal(long mealOrderId) {
        DeliveryOrder order = internalIdToOrder.get(mealOrderId);
        if(order != null) {
            mealsUnderPreparation.increment();
            log.debug("Kitchen started to prepare meal : {} for order: {}", order.name, order.id);
            return schedule(Chef.of(mealOrderId, order));
        }
        String template = "Unable to find the given cookReservationId id[%d]";
        return CompletableFuture.failedFuture(new NoSuchElementException(String.format(template, mealOrderId)));
    }

    private CompletableFuture<Boolean> schedule(Chef chef) {
        CompletableFuture<OrderPreparedEvent> mealReadyFuture = chef.prepareMeal();
        CompletableFuture<Boolean> publishedFuture = mealReadyFuture.thenApply(this::publishEventOrderReadyForPickup);
        publishedFuture.handleAsync((ev, ex) -> {
            log.debug("Kitchen's result of publishing meal ready event: {}", ev);
            mealsUnderPreparation.decrement();
            if(ex != null) {
                log.error("error while scheduling the order: {}", ex.getMessage(), ex);
            }
            return null;
        });
        return publishedFuture;
    }

    private boolean publishEventOrderReadyForPickup(OrderPreparedEvent readyForPickup) {
        try {
            if(null != this.publicNotificationDeque.get()) {
                this.publicNotificationDeque.get().offer(readyForPickup);
                return true;
            }
        } catch (Exception e) {
            log.error("Unable to notify food ready for pickup: {} ", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public long orderCookReservationId(DeliveryOrder order) {
        long mealOrderId = cookingOrderId.getAndIncrement();
        internalIdToOrder.put(mealOrderId, order);
        log.debug("Id requested for meal order: {} given: {}", order.id, mealOrderId);
        return mealOrderId;
    }

    @Override
    public boolean cancelCookReservation(long mealReservationId) {
        return null != internalIdToOrder.remove(mealReservationId);
    }

    @Override
    public void registerNotificationDeque(Deque<OutputEvent> deque) {
        this.publicNotificationDeque.set(deque);
    }

    @Override
    public boolean isKitchenIdle() {
        return this.mealsUnderPreparation.sum() == 0L;
    }

    private static class Chef {
        private final long mealOrderId;
        private final DeliveryOrder order;

        private Chef(long mealReservationId, DeliveryOrder order) {
            this.mealOrderId = mealReservationId;
            this.order = order;
        }

        private static Chef of(long mealOrderId, DeliveryOrder order) {
            return new Chef(mealOrderId, order);
        }

        private CompletableFuture<OrderPreparedEvent> prepareMeal() {
            return CompletableFuture.supplyAsync(() -> {
                        long now = KitchenClock.now();
                        return OrderPreparedEvent.of(this.mealOrderId, this.order.id, now);
                    },
                    CompletableFuture.delayedExecutor(order.prepTime, TimeUnit.MILLISECONDS));
        }
    }
}
