package com.acabra.orderfullfilment.orderserver.kitchen;

import com.acabra.orderfullfilment.orderserver.event.OrderPreparedEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;

import java.util.concurrent.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;

@Service
@Slf4j
public class KitchenServiceImpl implements KitchenService {
    private final AtomicLong cookingOrderId;
    private final ConcurrentHashMap<Long, DeliveryOrder> internalIdToOrder;
    private final AtomicReference<Deque<OutputEvent>> publicNotificationDeque;
    private final LongAdder mealsUnderPreparation;
    private final ScheduledExecutorService cookExecutor;
    private final PriorityBlockingQueue<MealSupplier> mealDeque;

    public KitchenServiceImpl() {
        PriorityBlockingQueue<MealSupplier> deque = new PriorityBlockingQueue<>();
        this.cookingOrderId = new AtomicLong();
        this.internalIdToOrder = new ConcurrentHashMap<>();
        this.publicNotificationDeque = new AtomicReference<>();
        this.mealsUnderPreparation = new LongAdder();
        this.mealDeque = deque;
        this.cookExecutor = buildCookExecutor(deque, mealsUnderPreparation);
    }

    private static ScheduledExecutorService buildCookExecutor(PriorityBlockingQueue<MealSupplier> mealDeque,
                                                              LongAdder completedCounter) {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            long now = KitchenClock.now();
            if (!mealDeque.isEmpty() && mealDeque.peek().isMealReady(now)) {
                try {
                    MealSupplier poll = mealDeque.poll(0L, TimeUnit.MILLISECONDS);
                    if(poll != null) {
                        poll.apply(now);
                        completedCounter.decrement();
                    }
                } catch (InterruptedException e) {
                    log.error("element in queue is null");
                }
            }
        }, 1000L, 900, TimeUnit.MILLISECONDS);
        return executorService;
    }

    @Override
    public CompletableFuture<Boolean> prepareMeal(long mealOrderId) {
        DeliveryOrder order = internalIdToOrder.get(mealOrderId);
        if(order != null) {
            mealsUnderPreparation.increment();
            log.debug("Kitchen started to prepare meal : {} for order: {}", order.name, order.id);
            Chef chef = Chef.of(mealOrderId, order, this.mealDeque);
            return schedule(chef);
        }
        String template = "Unable to find the given cookReservationId id[%d]";
        return CompletableFuture.failedFuture(new NoSuchElementException(String.format(template, mealOrderId)));
    }

    private CompletableFuture<Boolean> schedule(Chef chef) {
        return chef.prepareMeal(this.publicNotificationDeque.get());
    }

    @Override
    public long provideReservationId(DeliveryOrder order) {
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

    @Override
    public void shutdown() {
        this.cookExecutor.shutdownNow();
        log.info("Kitchen shutdown");
    }

    private static class Chef {
        private final long mealOrderId;
        private final DeliveryOrder order;
        private final PriorityBlockingQueue<MealSupplier> mealQueue;

        private Chef(long mealReservationId, DeliveryOrder order, PriorityBlockingQueue<MealSupplier> mealQueue) {
            this.mealOrderId = mealReservationId;
            this.order = order;
            this.mealQueue = mealQueue;
        }

        private static Chef of(long mealOrderId, DeliveryOrder order, PriorityBlockingQueue<MealSupplier> mealQueue) {
            return new Chef(mealOrderId, order, mealQueue);
        }

        private CompletableFuture<Boolean> prepareMeal(Deque<OutputEvent> deque) {
            MealSupplier mealSupplier = MealSupplier.of(this.mealOrderId, this.order.id, order.prepTime, deque);
            this.mealQueue.offer(mealSupplier);
            return mealSupplier.notifiedFuture;
        }
    }

    private static class MealSupplier implements Function<Long, Void>, Comparable<MealSupplier> {

        private final long mealOrderId;
        private final String id;
        private final long readyAt;
        private final Deque<OutputEvent> notificationQueue;
        private final CompletableFuture<Boolean> notifiedFuture;

        private MealSupplier(long mealOrderId, String id, long prepTime, Deque<OutputEvent> deque) {
            this.mealOrderId = mealOrderId;
            this.id = id;
            this.readyAt = prepTime + KitchenClock.now();
            this.notificationQueue = deque;
            notifiedFuture = new CompletableFuture<>();
        }

        public static MealSupplier of(long mealOrderId, String id, long prepTime, Deque<OutputEvent> deque) {
            return new MealSupplier(mealOrderId, id, prepTime, deque);
        }

        public boolean isMealReady(long now) {
            return now >= this.readyAt;
        }

        @Override
        public Void apply(Long now) {
            try {
                if(null != this.notificationQueue) {
                    this.notificationQueue.offer(OrderPreparedEvent.of(this.mealOrderId, this.id, now));
                    this.notifiedFuture.complete(true);
                    return null;
                } else {
                    log.error("error to publish meal prepared event no queue available");
                    this.notifiedFuture.complete(false);
                }
            } catch (Exception e) {
                String message = "Unable to notify food ready for pickup: " + e.getMessage();
                log.error(message, e);
                this.notifiedFuture.completeExceptionally(new Exception(message, e));
            }
            return null;
        }

        @Override
        public int compareTo(MealSupplier o) {
            return Long.compare(this.readyAt, o.readyAt);
        }
    }
}
