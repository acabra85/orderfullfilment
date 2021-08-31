package com.acabra.orderfullfilment.orderserver.kitchen;

import com.acabra.orderfullfilment.orderserver.event.OrderPreparedEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

@Service
@Slf4j
public class KitchenServiceImpl implements KitchenService {
    final AtomicLong cookingOrderId;
    private final ConcurrentHashMap<Long, DeliveryOrder> internalIdToOrder;
    private final AtomicReference<BlockingDeque<OutputEvent>> mealReadyNotificationDeque;
    private final LongAdder mealsUnderPreparation;

    public KitchenServiceImpl() {
        this.cookingOrderId = new AtomicLong();
        this.internalIdToOrder = new ConcurrentHashMap<>();
        this.mealReadyNotificationDeque = new AtomicReference<>();
        this.mealsUnderPreparation = new LongAdder();
    }

    private long nextOrderId() {
        return cookingOrderId.getAndIncrement();
    }

    @Override
    public CompletableFuture<Boolean> prepareMeal(long mealOrderId) {
        DeliveryOrder order = internalIdToOrder.get(mealOrderId);
        if(order != null) {
            mealsUnderPreparation.increment();
            log.debug("Kitchen started to prepare meal : {} for order: {}", order.name, order.id);
            return schedule(order, OrderPreparedEventSupplier.of(mealOrderId, order.id));
        }
        String template = "Unable to find the given cookReservationId id[%d]";
        return CompletableFuture.failedFuture(new NoSuchElementException(String.format(template, mealOrderId)));
    }

    private CompletableFuture<Boolean> schedule(DeliveryOrder order, OrderPreparedEventSupplier supplier) {
        CompletableFuture<Boolean> scheduleHandle = CompletableFuture
                .supplyAsync(supplier, CompletableFuture.delayedExecutor(order.prepTime, TimeUnit.MILLISECONDS))
                .thenApplyAsync(this::publishEventOrderReadyForPickup);
        scheduleHandle.handleAsync((ev, ex) -> {
            mealsUnderPreparation.decrement();
            return null;
        });
        return scheduleHandle;
    }

    private boolean publishEventOrderReadyForPickup(OrderPreparedEvent readyForPickup) {
        try {
            if(null != this.mealReadyNotificationDeque.get()) {
                this.mealReadyNotificationDeque.get().put(readyForPickup);
                return true;
            }
        } catch (InterruptedException e) {
            log.error("Unable to notify food ready for pickup: {} ", e.getMessage(), e);
        }
        return false;
    }

    @Override
    public long orderCookReservationId(DeliveryOrder order) {
        long mealOrderId = nextOrderId();
        internalIdToOrder.put(mealOrderId, order);
        log.debug("Id requested for meal order: {} given: {}", order.id, mealOrderId);
        return mealOrderId;
    }

    @Override
    public boolean cancelCookReservation(long mealReservationId) {
        return null != internalIdToOrder.remove(mealReservationId);
    }

    @Override
    public void registerMealNotificationReadyQueue(BlockingDeque<OutputEvent> deque) {
        this.mealReadyNotificationDeque.updateAndGet(oldValue -> deque);
    }

    @Override
    public boolean isKitchenIdle() {
        return this.mealsUnderPreparation.sum() == 0L;
    }
}
