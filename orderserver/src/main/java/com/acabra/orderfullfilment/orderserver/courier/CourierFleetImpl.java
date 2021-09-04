package com.acabra.orderfullfilment.orderserver.courier;

import com.acabra.orderfullfilment.orderserver.event.CourierArrivedEvent;
import com.acabra.orderfullfilment.orderserver.courier.model.Courier;
import com.acabra.orderfullfilment.orderserver.courier.model.DispatchResult;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import com.acabra.orderfullfilment.orderserver.utils.EtaEstimator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;
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
    private final Deque<Courier> availableCouriers;

    private final AtomicInteger totalCouriers;
    private final AtomicReference<BlockingDeque<OutputEvent>> courierAvailableNotificationDeque;
    private final EtaEstimator etaEstimator;

    private Courier findFirstAvailable() {
        if(availableCouriers.isEmpty()) throw new NoSuchElementException("No Available couriers");
        return availableCouriers.remove();
    }

    private Courier retrieveCourier() {
        Courier courier = null;
        if (availableCouriers.size() > 0) {
            courier = findFirstAvailable();
            courier.dispatch();
        }
        return courier;
    }

    public CourierFleetImpl(List<Courier> couriers, EtaEstimator etaEstimator) {
        this.availableCouriers = couriers.stream().filter(Courier::isAvailable).collect(DEQUE_COLLECTOR);
        this.dispatchedCouriers = buildDispatchedMap(couriers);
        this.totalCouriers = new AtomicInteger(couriers.size());
        this.courierAvailableNotificationDeque = new AtomicReference<>();
        this.etaEstimator = etaEstimator;
    }

    private Map<Integer, Courier> buildDispatchedMap(List<Courier> couriers) {
        return this.availableCouriers.size() != couriers.size() ?
                couriers.stream()
                        .filter(c -> !c.isAvailable())
                        .map(c-> Map.entry(c.id, c))
                        .collect(
                            Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                : new HashMap<>();
    }

    @Override
    synchronized public DispatchResult dispatch(DeliveryOrder order) {
        Courier courier = retrieveCourier();
        if(null != courier) {
            dispatchedCouriers.put(courier.id, courier);
            int eta = etaEstimator.estimateCourierTravelTimeInSeconds(courier);
            CompletableFuture<Boolean> schedule = this.schedule(eta, courier.id);
            return DispatchResult.of(courier.id, schedule);
        }
        return DispatchResult.notDispatched();
    }

    @Override
    synchronized public void release(Integer courierId) throws NoSuchElementException {
        Courier courier = this.dispatchedCouriers.get(courierId);
        if(null == courier) {
            String error = String.format("The given id [%d] does not correspond to an assigned courier", courierId);
            throw new NoSuchElementException(error);
        }
        courier.orderDelivered();
        this.dispatchedCouriers.put(courierId, null);
        this.availableCouriers.add(courier);
        log.debug("Courier[{},{}] is available ... remaining available couriers: {} ", courierId, courier.name, this.availableCouriers.size());
    }

    private CompletableFuture<Boolean> schedule(long timeToDestination, int courierId) {
        long eta = KitchenClock.now() + 1000L * timeToDestination;
        return CompletableFuture.supplyAsync(() -> {
                    CourierArrivedEvent pickupEvent = CourierArrivedEvent.of(courierId, eta, KitchenClock.now());
                    log.info("[EVENT] courier arrived id[{}], for pickup at {}ms", pickupEvent.courierId,
                            KitchenClock.formatted(pickupEvent.createdAt));
                    try {
                        if(courierAvailableNotificationDeque.get() != null) {
                            courierAvailableNotificationDeque.get().put(pickupEvent);
                            return true;
                        }
                    } catch (InterruptedException e) {
                        log.error("Failed to publish the notification");
                    }
                    return false;
                }, CompletableFuture.delayedExecutor(timeToDestination, TimeUnit.SECONDS));
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
    public void registerNotificationDeque(BlockingDeque<OutputEvent> deque) {
        courierAvailableNotificationDeque.set(deque);
    }
}
