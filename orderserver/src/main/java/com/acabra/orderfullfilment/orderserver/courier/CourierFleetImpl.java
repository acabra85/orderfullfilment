package com.acabra.orderfullfilment.orderserver.courier;

import com.acabra.orderfullfilment.orderserver.courier.model.Courier;
import com.acabra.orderfullfilment.orderserver.courier.model.DispatchResult;
import com.acabra.orderfullfilment.orderserver.event.CourierArrivedEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import com.acabra.orderfullfilment.orderserver.utils.EtaEstimator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Slf4j
@Component
public class CourierFleetImpl implements CourierFleet {

    private static final Collector<Courier, ?, ArrayDeque<Courier>> DEQUE_COLLECTOR = Collectors.toCollection(ArrayDeque::new);

    private final Map<Integer, Courier> dispatchedCouriers;
    private final ConcurrentLinkedDeque<Courier> availableCouriers;

    private final AtomicInteger totalCouriers;
    private final AtomicReference<Queue<OutputEvent>> pubDeque;
    private final EtaEstimator etaEstimator;

    public CourierFleetImpl(List<Courier> couriers, EtaEstimator etaEstimator) {
        this.availableCouriers = new ConcurrentLinkedDeque<>(couriers.stream()
                .filter(Courier::isAvailable).collect(DEQUE_COLLECTOR));
        this.dispatchedCouriers = buildDispatchedMap(couriers);
        this.totalCouriers = new AtomicInteger(couriers.size());
        this.pubDeque = new AtomicReference<>();
        this.etaEstimator = etaEstimator;
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
    public DispatchResult dispatch(DeliveryOrder order, long now) {
        Courier courier = getAvailableCourier();
        courier.dispatch();
        dispatchedCouriers.put(courier.id, courier);
        long ettMillis = etaEstimator.estimateCourierTravelTimeInSeconds(courier) * 1000L;
        CompletableFuture<Boolean> emulated = emulateDispatched(courier.id, ettMillis, now + ettMillis);
        return DispatchResult.of(courier.id, emulated, ettMillis);
    }

    private CompletableFuture<Boolean> emulateDispatched(int courierId, long ett, long now) {
        return CompletableFuture.supplyAsync(() -> reportCourierArrived(CourierArrivedEvent.of(courierId, ett, now)),
                CompletableFuture.delayedExecutor(10L, TimeUnit.MILLISECONDS));
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
    }

    private boolean reportCourierArrived(OutputEvent outputEvent) {
        return publish(outputEvent);
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
}
