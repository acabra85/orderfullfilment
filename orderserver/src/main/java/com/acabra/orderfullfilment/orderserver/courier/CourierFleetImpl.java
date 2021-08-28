package com.acabra.orderfullfilment.orderserver.courier;

import com.acabra.orderfullfilment.orderserver.config.CourierConfig;
import com.acabra.orderfullfilment.orderserver.courier.event.CourierReadyForPickupEvent;
import com.acabra.orderfullfilment.orderserver.courier.model.AssignmentDetails;
import com.acabra.orderfullfilment.orderserver.courier.model.Courier;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class CourierFleetImpl implements CourierFleet {

    private final HashMap<Integer, Courier> dispatchedCouriers = new HashMap<>();
    private final Deque<Courier> availableCouriers;

    private final int ceilingEta;
    private final Integer floorEta;
    private final static Random etaGenerate = new Random();
    private final CourierDispatchService dispatchService;

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

    public CourierFleetImpl(List<Courier> availableCouriers, CourierConfig config,
                              CourierDispatchService dispatchService) {
        this.availableCouriers = new ArrayDeque<>(availableCouriers);
        this.ceilingEta = config.getMaxEta() - config.getMinEta() + 1;
        this.floorEta = config.getMinEta();
        this.dispatchService = dispatchService;
    }

    @Override
    synchronized public Integer dispatch(DeliveryOrder order) {
        Courier courier = retrieveCourier();
        if(null != courier) {
            dispatchedCouriers.put(courier.id, courier);
            AssignmentDetails pending = AssignmentDetails.pending(calculateArrivalTime());
            this.schedule(pending.eta, courier.id);
            return courier.id;
        }
        return null;
    }

    private int calculateArrivalTime() {
        return Math.abs(floorEta + etaGenerate.nextInt(ceilingEta));
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
        log.debug("Courier {} is available ... ", courierId);
    }

    private void schedule(long timeToDestination, int courierId) {
        long eta = System.currentTimeMillis() + 1000L * timeToDestination;
        CompletableFuture.supplyAsync(() -> {
                    CourierReadyForPickupEvent pickupEvent = new CourierReadyForPickupEvent(courierId, eta, KitchenClock.now());
                    log.info("[EVENT] Courier {} arrived for pickup at {}ms \n", pickupEvent.courierId,
                            KitchenClock.formatted(pickupEvent.arrivalTime));
                    return pickupEvent;
                }, CompletableFuture.delayedExecutor(timeToDestination, TimeUnit.SECONDS)
        ).thenAccept(dispatchService::processCourierArrival);
    }
}
