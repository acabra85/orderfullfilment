package com.acabra.orderfullfilment.orderserver.courier;

import com.acabra.orderfullfilment.orderserver.core.CompletableTask;
import com.acabra.orderfullfilment.orderserver.core.executor.SafeTask;
import com.acabra.orderfullfilment.orderserver.courier.model.Courier;
import com.acabra.orderfullfilment.orderserver.courier.model.DispatchResult;
import com.acabra.orderfullfilment.orderserver.event.CourierArrivedEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import com.acabra.orderfullfilment.orderserver.order.CompletableTaskMonitor;
import com.acabra.orderfullfilment.orderserver.utils.EtaEstimator;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CourierFleetImpl implements CourierFleet {

    private static final Collector<Courier, ?, ArrayDeque<Courier>> DEQUE_COLLECTOR = Collectors.toCollection(ArrayDeque::new);

    private final Map<Integer, Courier> dispatchedCouriers;
    private final ConcurrentLinkedDeque<Courier> availableCouriers;

    private final AtomicInteger totalCouriers;
    private final AtomicReference<Deque<OutputEvent>> pubDeque;
    private final EtaEstimator etaEstimator;
    private final PriorityBlockingQueue<CompletableTask> scheduleDeque;
    private final ScheduledExecutorService dispatchExecutor;

    public CourierFleetImpl(List<Courier> couriers, EtaEstimator etaEstimator) {
        PriorityBlockingQueue<CompletableTask> dispatchDeque = new PriorityBlockingQueue<>();
        this.availableCouriers = new ConcurrentLinkedDeque<>(couriers.stream()
                .filter(Courier::isAvailable).collect(DEQUE_COLLECTOR));
        this.dispatchedCouriers = buildDispatchedMap(couriers);
        this.totalCouriers = new AtomicInteger(couriers.size());
        this.pubDeque = new AtomicReference<>();
        this.etaEstimator = etaEstimator;
        this.scheduleDeque = dispatchDeque;
        this.dispatchExecutor = startMonitor(CompletableTaskMonitor.of(dispatchDeque));
    }

    private Map<Integer, Courier> buildDispatchedMap(List<Courier> couriers) {
        return new ConcurrentHashMap<>(this.availableCouriers.size() != couriers.size() ?
                couriers.stream()
                        .filter(c -> !c.isAvailable())
                        .map(c-> Map.entry(c.id, c))
                        .collect(
                            Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                : new HashMap<>());
    }

    @Override
    public DispatchResult dispatch(DeliveryOrder order) {
        Courier courier = getAvailableCourier();
        courier.dispatch();
        dispatchedCouriers.put(courier.id, courier);
        int estimatedTravelTime = etaEstimator.estimateCourierTravelTimeInSeconds(courier);
        return DispatchResult.of(courier.id, schedule(estimatedTravelTime, courier.id), estimatedTravelTime);
    }

    private Courier getAvailableCourier() {
        Courier poll = availableCouriers.poll();
        if(poll == null) {
            int id = this.totalCouriers.incrementAndGet();
            return Courier.ofAvailable(id, "CourierName:" + id);
        }
        return poll;
    }

    @Override
    public void release(Integer courierId) throws NoSuchElementException {
        Courier courier = this.dispatchedCouriers.get(courierId);
        if(null == courier) {
            String error = String.format("The given id [%d] does not correspond to an assigned courier", courierId);
            throw new NoSuchElementException(error);
        }
        courier.orderDelivered();
        this.dispatchedCouriers.remove(courierId);
        this.availableCouriers.offer(courier);
        log.debug("Courier[{},{}] is available ... remaining available couriers: {} ", courierId, courier.name, this.availableCouriers.size());
    }

    private CompletableFuture<Boolean> schedule(int timeToDestination, int courierId) {
        CompletableTask scheduledArrival = buildCompletableTask(courierId, timeToDestination * 1000L,
                this::reportCourierArrived);
        scheduleDeque.offer(scheduledArrival);
        return scheduledArrival.getCompletionFuture();
    }

    private boolean reportCourierArrived(OutputEvent outputEvent) {
        return publish(outputEvent);
    }

    private static ScheduledExecutorService startMonitor(SafeTask safeTask) {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(safeTask, 0, 900, TimeUnit.MILLISECONDS);
        return executorService;
    }

    @Override
    public int fleetSize() {
        return totalCouriers.get();
    }

    @Override
    public int availableCouriers() {
        return availableCouriers.size();
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
    public void shutdown() {
        this.dispatchExecutor.shutdownNow();
    }

    private CompletableTask buildCompletableTask(int courierId, long eta, Function<OutputEvent, Boolean> report) {
        return new CompletableTask() {
            public final long arrivalAt = KitchenClock.now() + eta;
            public final CompletableFuture<Boolean> completedFuture = new CompletableFuture<>();

            @Override
            public long expectedCompletionAt() {
                return arrivalAt;
            }

            @Override
            public void accept(Long now) {
                OutputEvent evt = CourierArrivedEvent.of(courierId, arrivalAt, now);
                this.completedFuture.complete(report.apply(evt));
            }

            @Override
            public CompletableFuture<Boolean> getCompletionFuture() {
                return this.completedFuture;
            }
        };
    }
}
