package com.acabra.orderfullfilment.orderserver.kitchen;

import com.acabra.orderfullfilment.orderserver.event.OrderPreparedEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;

@Service
@Slf4j
public class KitchenServiceImpl implements KitchenService {
    private final AtomicLong kitchenReservationIds;
    private final ConcurrentHashMap<Long, DeliveryOrder> internalIdToOrder;
    private final AtomicReference<Queue<OutputEvent>> pubDeque;
    private final LongAdder mealsUnderPreparation;

    public KitchenServiceImpl() {
        this.kitchenReservationIds = new AtomicLong();
        this.internalIdToOrder = new ConcurrentHashMap<>();
        this.pubDeque = new AtomicReference<>();
        this.mealsUnderPreparation = new LongAdder();
    }

    private boolean reportMealPrepared(OutputEvent outputEvent) {
        mealsUnderPreparation.decrement();
        return publish(outputEvent);
    }

    @Override
    public CompletableFuture<Boolean> prepareMeal(long kitchenReservationId, long now) {
        DeliveryOrder order = internalIdToOrder.get(kitchenReservationId);
        if(order != null) {
            mealsUnderPreparation.increment();
            return emulatedPreparation(kitchenReservationId, order, now + order.prepTime);
        }
        String template = "Unable to find the given cookReservationId id[%d]";
        return CompletableFuture.failedFuture(new NoSuchElementException(String.format(template, kitchenReservationId)));
    }

    private CompletableFuture<Boolean> emulatedPreparation(long id, DeliveryOrder order, long readyAt) {
        OrderPreparedEvent event = OrderPreparedEvent.of(id, order.id, readyAt);
        return CompletableFuture.supplyAsync(() -> reportMealPrepared(event),
                CompletableFuture.delayedExecutor(10L, TimeUnit.MILLISECONDS));
    }

    @Override
    public long provideReservationId(DeliveryOrder order) {
        long kitchenReservationId = kitchenReservationIds.getAndIncrement();
        internalIdToOrder.put(kitchenReservationId, order);
        return kitchenReservationId;
    }

    @Override
    public boolean cancelCookReservation(long kitchenReservationId) {
        return null != internalIdToOrder.remove(kitchenReservationId);
    }

    @Override
    public void registerNotificationDeque(Queue<OutputEvent> deque) {
        this.pubDeque.set(deque);
    }

    @Override
    public Queue<OutputEvent> getPubDeque() {
        return this.pubDeque.get();
    }

    @Override
    public void logError(String msg, Throwable e) {
        log.error(msg, e);
    }

    @Override
    public boolean isKitchenIdle() {
        return this.mealsUnderPreparation.sum() == 0L;
    }

    @Override
    public void shutdown() {
        log.info("Kitchen shutdown");
    }
}
