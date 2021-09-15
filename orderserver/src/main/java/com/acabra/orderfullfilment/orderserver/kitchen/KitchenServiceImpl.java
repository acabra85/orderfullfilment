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
    private final PriorityBlockingQueue<Chef> mealDeque;

    public KitchenServiceImpl() {
        PriorityBlockingQueue<Chef> deque = new PriorityBlockingQueue<>();
        this.cookingOrderId = new AtomicLong();
        this.internalIdToOrder = new ConcurrentHashMap<>();
        this.publicNotificationDeque = new AtomicReference<>();
        this.mealsUnderPreparation = new LongAdder();
        this.mealDeque = deque;
        this.cookExecutor = buildCookExecutor(deque, mealsUnderPreparation);
    }

    private static ScheduledExecutorService buildCookExecutor(PriorityBlockingQueue<Chef> mealDeque,
                                                              LongAdder completedCounter) {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            long now = KitchenClock.now();
            if (!mealDeque.isEmpty() && mealDeque.peek().isMealReady(now)) {
                try {
                    Chef chef = mealDeque.poll(0L, TimeUnit.MILLISECONDS);
                    if(chef != null) {
                        chef.publishEvent(now);
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
            Chef chef = Chef.of(mealOrderId, order.id, order.prepTime, this.publicNotificationDeque.get());
            this.mealDeque.offer(chef);
            return chef.mealFuture;
        }
        String template = "Unable to find the given cookReservationId id[%d]";
        return CompletableFuture.failedFuture(new NoSuchElementException(String.format(template, mealOrderId)));
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


    private static class Chef implements Comparable<Chef> {

        private final long mealOrderId;
        private final String id;
        private final long readyAt;
        private final Deque<OutputEvent> notificationQueue;
        private final CompletableFuture<Boolean> mealFuture;

        private Chef(long mealOrderId, String id, long prepTime, Deque<OutputEvent> deque) {
            this.mealOrderId = mealOrderId;
            this.id = id;
            this.readyAt = prepTime + KitchenClock.now();
            this.notificationQueue = deque;
            this.mealFuture = new CompletableFuture<>();
        }

        public static Chef of(long mealOrderId, String id, long prepTime, Deque<OutputEvent> deque) {
            return new Chef(mealOrderId, id, prepTime, deque);
        }

        public boolean isMealReady(long now) {
            return now >= this.readyAt;
        }

        public void publishEvent(Long now) {
            try {
                if(null != this.notificationQueue) {
                    this.notificationQueue.offer(OrderPreparedEvent.of(this.mealOrderId, this.id, now));
                    this.mealFuture.complete(true);
                } else {
                    log.error("error to publish meal prepared event no queue available");
                    this.mealFuture.complete(false);
                }
            } catch (Exception e) {
                String message = "Unable to notify food ready for pickup: " + e.getMessage();
                log.error(message, e);
                this.mealFuture.completeExceptionally(new Exception(message, e));
            }
        }

        @Override
        public int compareTo(Chef o) {
            return Long.compare(this.readyAt, o.readyAt);
        }
    }
}
