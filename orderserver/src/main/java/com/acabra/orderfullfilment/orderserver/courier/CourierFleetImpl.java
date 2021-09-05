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
import java.util.concurrent.*;
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
    private final AtomicReference<Deque<OutputEvent>> courierAvailableNotificationDeque;
    private final EtaEstimator etaEstimator;

    public CourierFleetImpl(List<Courier> couriers, EtaEstimator etaEstimator) {
        this.availableCouriers = new ConcurrentLinkedDeque<>(couriers.stream()
                .filter(Courier::isAvailable).collect(DEQUE_COLLECTOR));
        this.dispatchedCouriers = buildDispatchedMap(couriers);
        this.totalCouriers = new AtomicInteger(couriers.size());
        this.courierAvailableNotificationDeque = new AtomicReference<>();
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
    public DispatchResult dispatch(DeliveryOrder order) {
        Courier courier = availableCouriers.poll();
        if(null != courier) {
            dispatchedCouriers.put(courier.id, courier);
            int estimatedTravelTime = etaEstimator.estimateCourierTravelTimeInSeconds(courier);
            CompletableFuture<Boolean> schedule = this.schedule(estimatedTravelTime, courier.id);
            return DispatchResult.of(courier.id, schedule, estimatedTravelTime);
        }
        return DispatchResult.notDispatched();
    }

    @Override
    public void release(Integer courierId) throws NoSuchElementException {
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
                    try {
                        if(courierAvailableNotificationDeque.get() != null) {
                            courierAvailableNotificationDeque.get().offer(pickupEvent);
                            return true;
                        }
                    } catch (Exception e) {
                        log.error("Failed to publish the notification: {}", e.getMessage(), e);
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
    public void registerNotificationDeque(Deque<OutputEvent> deque) {
        courierAvailableNotificationDeque.set(deque);
    }
}
