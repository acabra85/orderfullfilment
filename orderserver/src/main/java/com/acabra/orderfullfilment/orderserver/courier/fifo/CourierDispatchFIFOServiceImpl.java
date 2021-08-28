package com.acabra.orderfullfilment.orderserver.courier.fifo;

import com.acabra.orderfullfilment.orderserver.courier.CourierDispatchService;
import com.acabra.orderfullfilment.orderserver.courier.CourierFleet;
import com.acabra.orderfullfilment.orderserver.courier.event.CourierReadyForPickupEvent;
import com.acabra.orderfullfilment.orderserver.courier.event.PickupCompletedEvent;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import com.acabra.orderfullfilment.orderserver.kitchen.event.MealReadyForPickupEvent;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    private final LinkedBlockingDeque<CourierReadyForPickupEvent> couriersAwaitingPickup = new LinkedBlockingDeque<>();
    private final CourierFleet courierFleet;

    public CourierDispatchFIFOServiceImpl(CourierFleet courierFleet) {
        this.courierFleet = courierFleet;
    }

    @Override
    public Optional<Integer> dispatchRequest(DeliveryOrder order) {
        Integer courierId = this.courierFleet.dispatch(null == order ? Optional.empty() : Optional.of(order));
        if(null != courierId) {
            return Optional.of(courierId);
        }
        return Optional.empty();
    }

    @Override
    public void processCourierArrival(CourierReadyForPickupEvent pickupEvent) {
        couriersAwaitingPickup.addLast(pickupEvent);
    }

    @Override
    public void processMealReady(MealReadyForPickupEvent mealReadyEvent) {
        MealAwaitingPickupSupplier supplier = new MealAwaitingPickupSupplier(couriersAwaitingPickup, mealReadyEvent);
        CompletableFuture.supplyAsync(supplier)
                .thenApply(pickupCompletedEvent -> {
                    recordMetrics(mealReadyEvent, pickupCompletedEvent);
                    return pickupCompletedEvent.courierId;
                })
                .thenAcceptAsync(courierFleet::release);
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
