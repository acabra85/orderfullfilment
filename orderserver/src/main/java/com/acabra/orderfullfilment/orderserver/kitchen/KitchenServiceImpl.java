package com.acabra.orderfullfilment.orderserver.kitchen;

import com.acabra.orderfullfilment.orderserver.core.executor.SafeTask;
import com.acabra.orderfullfilment.orderserver.event.OrderPreparedEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import lombok.SneakyThrows;
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
    private final AtomicLong cookingOrderId;
    private final ConcurrentHashMap<Long, DeliveryOrder> internalIdToOrder;
    private final AtomicReference<Deque<OutputEvent>> pubDeque;
    private final LongAdder mealsUnderPreparation;
    private final ScheduledExecutorService cookExecutor;
    private final PriorityBlockingQueue<Chef> mealDeque;

    public KitchenServiceImpl() {
        PriorityBlockingQueue<Chef> deque = new PriorityBlockingQueue<>();
        this.cookingOrderId = new AtomicLong();
        this.internalIdToOrder = new ConcurrentHashMap<>();
        this.pubDeque = new AtomicReference<>();
        this.mealsUnderPreparation = new LongAdder();
        this.mealDeque = deque;
        this.cookExecutor = buildCookExecutor(buildSafeTask(deque));
    }

    private static SafeTask buildSafeTask(PriorityBlockingQueue<Chef> deque) {
        return new SafeTask() {
            @SneakyThrows
            @Override
            protected void doWork() {
                long now = KitchenClock.now();
                if (!deque.isEmpty() && deque.peek().isMealReady(now)) {
                    Chef chef = deque.poll(0L, TimeUnit.MILLISECONDS);
                    if(chef != null) {
                        chef.reportMealReady(now);
                    }
                }
            }
        };
    }

    private static ScheduledExecutorService buildCookExecutor(SafeTask safeTask) {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(safeTask, 1000L, 900, TimeUnit.MILLISECONDS);
        return executorService;
    }

    @Override
    public CompletableFuture<Boolean> prepareMeal(long mealOrderId) {
        DeliveryOrder order = internalIdToOrder.get(mealOrderId);
        if(order != null) {
            mealsUnderPreparation.increment();
            log.debug("Kitchen started to prepare meal : {} for order: {}", order.name, order.id);
            Chef prepare = Chef.prepare(mealOrderId, order, this::reportMealPrepared);
            CompletableFuture<Boolean> notificationHandle = prepare.readyFuture;
            this.mealDeque.offer(prepare);
            return notificationHandle;
        }
        String template = "Unable to find the given cookReservationId id[%d]";
        return CompletableFuture.failedFuture(new NoSuchElementException(String.format(template, mealOrderId)));
    }

    private boolean reportMealPrepared(OutputEvent outputEvent) {
        mealsUnderPreparation.decrement();
        return publish(outputEvent);
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
        this.pubDeque.set(deque);
    }

    @Override
    public AtomicReference<Deque<OutputEvent>> getPubDeque() {
        return this.pubDeque;
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
        this.cookExecutor.shutdownNow();
        log.info("Kitchen shutdown");
    }

    private static class Chef implements Comparable<Chef> {
        private final long mealOrderId;
        private final String id;
        private final long readyAt;
        public final CompletableFuture<Boolean> readyFuture = new CompletableFuture<>();
        private final Function<OutputEvent, Boolean> report;

        private Chef(long mealOrderId, String id, long prepTimeMillis, Function<OutputEvent, Boolean> report) {
            this.mealOrderId = mealOrderId;
            this.id = id;
            this.readyAt = prepTimeMillis + KitchenClock.now();
            this.report = report;
        }

        public static Chef prepare(long mealOrderId, DeliveryOrder order, Function<OutputEvent, Boolean> report) {
            return new Chef(mealOrderId, order.id, order.prepTime, report);
        }

        public boolean isMealReady(long now) {
            return now >= this.readyAt;
        }

        public void reportMealReady(Long now) {
            OrderPreparedEvent event = OrderPreparedEvent.of(this.mealOrderId, this.id, now);
            this.readyFuture.complete(this.report.apply(event));
        }

        @Override
        public int compareTo(Chef o) {
            return Long.compare(this.readyAt, o.readyAt);
        }
    }
}
