package com.acabra.orderfullfilment.orderserver.courier.fifo;

import com.acabra.orderfullfilment.orderserver.courier.CourierDispatchService;
import com.acabra.orderfullfilment.orderserver.courier.CourierFleet;
import com.acabra.orderfullfilment.orderserver.courier.event.CourierReadyForPickupEvent;
import com.acabra.orderfullfilment.orderserver.courier.event.PickupCompletedEvent;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import com.acabra.orderfullfilment.orderserver.kitchen.event.MealReadyForPickupEvent;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.LongSummaryStatistics;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "dispatch", name = "strategy", havingValue = "fifo")
public class CourierDispatchFIFOServiceImpl implements CourierDispatchService {

    private final LongSummaryStatistics foodWaitTimeStats = new LongSummaryStatistics();
    private final LongSummaryStatistics courierWaitTimeStats = new LongSummaryStatistics();
    private final LinkedBlockingDeque<CourierReadyForPickupEvent> couriersAwaitingPickup;
    private final CourierFleet courierFleet;

    @Autowired
    public CourierDispatchFIFOServiceImpl(CourierFleet courierFleet) {
        this.courierFleet = courierFleet;
        this.couriersAwaitingPickup = new LinkedBlockingDeque<>();
    }

    @Autowired(required = false)
    public CourierDispatchFIFOServiceImpl(CourierFleet courierFleet,
                                          LinkedBlockingDeque<CourierReadyForPickupEvent> blockingQue) {
        this.courierFleet = courierFleet;
        this.couriersAwaitingPickup = blockingQue;
    }

    @Override
    public Optional<Integer> dispatchRequest(DeliveryOrder order) {
        Integer courierId = this.courierFleet.dispatch(order);
        if(null != courierId) {
            return Optional.of(courierId);
        }
        return Optional.empty();
    }

    @Override
    public boolean processCourierArrival(CourierReadyForPickupEvent pickupEvent) {
        if(null == pickupEvent) {
            return false;
        }
        try {
            couriersAwaitingPickup.addLast(pickupEvent);
            return true;
        } catch (IllegalStateException ise) {
            log.error(ise.getMessage(), ise);
        }
        return false;
    }

    @Override
    public CompletableFuture<Void> processMealReady(MealReadyForPickupEvent mealReadyEvent) {
        CompletableFuture<Integer> future = CompletableFuture
                .supplyAsync(new MealAwaitingPickupSupplier(couriersAwaitingPickup, mealReadyEvent.readySince))
                .thenApply(ev -> {
                    recordMetrics(mealReadyEvent, ev);
                    return ev.courierId;
                });
        return future.handleAsync((id, ex) -> {
            if(id != null) {
                courierFleet.release(id);
            } else {
                log.error("Error processing meal ready event: {}", ex.getMessage(), ex);
            }
            return null;
        });
    }

    private void recordMetrics(MealReadyForPickupEvent mealReadyEvent, PickupCompletedEvent pickupCompletedEvent) {
        foodWaitTimeStats.accept(pickupCompletedEvent.foodWaitTime);
        courierWaitTimeStats.accept(pickupCompletedEvent.courierWaitTime);
        courierWaitTimeStats.accept(pickupCompletedEvent.courierWaitTime);
        log.info("[EVENT] Order {} picked by Courier {} at {}",
                mealReadyEvent.deliveryOrderId,
                pickupCompletedEvent.courierId,
                KitchenClock.formatted(pickupCompletedEvent.at));
        log.info("[METRICS] Food wait time: {}ms id[{}].",
                pickupCompletedEvent.foodWaitTime,
                mealReadyEvent.deliveryOrderId);
        log.info("[METRICS] Courier wait time: {}ms for order {}\n",
                pickupCompletedEvent.courierWaitTime,
                mealReadyEvent.deliveryOrderId);
    }
}
