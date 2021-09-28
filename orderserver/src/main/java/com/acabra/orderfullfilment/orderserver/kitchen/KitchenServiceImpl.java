package com.acabra.orderfullfilment.orderserver.kitchen;

import com.acabra.orderfullfilment.orderserver.core.CompletableTask;
import com.acabra.orderfullfilment.orderserver.core.executor.SchedulerExecutorAssistant;
import com.acabra.orderfullfilment.orderserver.event.OrderPreparedEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import com.acabra.orderfullfilment.orderserver.order.CompletableTaskMonitor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

@Service
@Slf4j
public class KitchenServiceImpl implements KitchenService {
    private final AtomicLong kitchenReservationIds;
    private final ConcurrentHashMap<Long, DeliveryOrder> internalIdToOrder;
    private final AtomicReference<Deque<OutputEvent>> pubDeque;
    private final LongAdder mealsUnderPreparation;
    private final PriorityBlockingQueue<CompletableTask> mealDeque;

    public KitchenServiceImpl(SchedulerExecutorAssistant scheduler) {
        this.kitchenReservationIds = new AtomicLong();
        this.internalIdToOrder = new ConcurrentHashMap<>();
        this.pubDeque = new AtomicReference<>();
        this.mealsUnderPreparation = new LongAdder();
        this.mealDeque = new PriorityBlockingQueue<>();
        //schedule monitoring meal deque
        scheduler.scheduleAtFixedRate(CompletableTaskMonitor.of(this.mealDeque), 1000L, 500L);
    }

    private boolean reportMealPrepared(OutputEvent outputEvent) {
        mealsUnderPreparation.decrement();
        return publish(outputEvent);
    }

    @Override
    public CompletableFuture<Boolean> prepareMeal(long kitchenReservationId) {
        DeliveryOrder order = internalIdToOrder.get(kitchenReservationId);
        if(order != null) {
            mealsUnderPreparation.increment();
            log.debug("Kitchen started to prepare meal : {} for order: {}", order.name, order.id);
            return schedule(kitchenReservationId, order);
        }
        String template = "Unable to find the given cookReservationId id[%d]";
        return CompletableFuture.failedFuture(new NoSuchElementException(String.format(template, kitchenReservationId)));
    }

    private CompletableFuture<Boolean> schedule(long kitchenReservationId, DeliveryOrder order) {
        CompletableTask scheduledMeal = buildCompletableTask(kitchenReservationId, order, this::reportMealPrepared);
        this.mealDeque.offer(scheduledMeal);
        return scheduledMeal.getCompletionFuture();
    }

    @Override
    public long provideReservationId(DeliveryOrder order) {
        long kitchenReservationId = kitchenReservationIds.getAndIncrement();
        internalIdToOrder.put(kitchenReservationId, order);
        log.debug("Id requested for meal order: {} given: {}", order.id, kitchenReservationId);
        return kitchenReservationId;
    }

    @Override
    public boolean cancelCookReservation(long kitchenReservationId) {
        return null != internalIdToOrder.remove(kitchenReservationId);
    }

    @Override
    public void registerNotificationDeque(Deque<OutputEvent> deque) {
        this.pubDeque.set(deque);
    }

    @Override
    public Deque<OutputEvent> getPubDeque() {
        return this.pubDeque.get();
    }

    @Override
    public Logger log() {
        return log;
    }

    @Override
    public boolean isKitchenIdle() {
        return this.mealsUnderPreparation.sum() == 0L;
    }

    @Override
    public void shutdown() {
        log.info("Kitchen shutdown");
    }

    private CompletableTask buildCompletableTask(long reserveId, DeliveryOrder order, Function<OutputEvent, Boolean> report) {
        return new CompletableTask() {
            public final CompletableFuture<Boolean> completionFuture = new CompletableFuture<>();
            private final long readyAt =  order.prepTime + KitchenClock.now();

            @Override
            public long expectedCompletionAt() {
                return this.readyAt;
            }

            @Override
            public void accept(Long now) {
                OutputEvent evt = OrderPreparedEvent.of(reserveId, order.id, now);
                this.completionFuture.complete(report.apply(evt));
            }

            @Override
            public CompletableFuture<Boolean> getCompletionFuture() {
                return this.completionFuture;
            }
        };
    }
}
