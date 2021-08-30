package com.acabra.orderfullfilment.orderserver.courier.fifo;

import com.acabra.orderfullfilment.orderserver.courier.CourierDispatchService;
import com.acabra.orderfullfilment.orderserver.courier.CourierFleet;
import com.acabra.orderfullfilment.orderserver.event.CourierArrivedEvent;
import com.acabra.orderfullfilment.orderserver.event.OrderPickedUpEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import com.acabra.orderfullfilment.orderserver.event.OrderPreparedEvent;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.LongSummaryStatistics;
import java.util.Optional;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "dispatch", name = "strategy", havingValue = "fifo")
public class CourierDispatchFIFOServiceImpl implements CourierDispatchService {

    private final LongSummaryStatistics foodWaitTimeStats = new LongSummaryStatistics();
    private final LongSummaryStatistics courierWaitTimeStats = new LongSummaryStatistics();
    private final BlockingDeque<OutputEvent> couriersAwaitingPickup;
    private final CourierFleet courierFleet;

    @Autowired
    public CourierDispatchFIFOServiceImpl(CourierFleet courierFleet) {
        final BlockingDeque<OutputEvent> deque = new LinkedBlockingDeque<>();
        this.courierFleet = courierFleet;
        this.couriersAwaitingPickup = deque;
        this.courierFleet.registerNotificationDeque(deque);
    }

    public CourierDispatchFIFOServiceImpl(CourierFleet courierFleet,
                                          BlockingDeque<OutputEvent> blockingDeque) {
        this.courierFleet = courierFleet;
        this.couriersAwaitingPickup = blockingDeque;
        this.courierFleet.registerNotificationDeque(blockingDeque);
    }

    @Override
    public Optional<Integer> dispatchRequest(DeliveryOrder order) {
        Integer courierId = this.courierFleet.dispatch(order).courierId;
        if(null != courierId) {
            log.info("[EVENT] Courier dispatched: id[{}], {} available", courierId,
                this.courierFleet.availableCouriers());
            return Optional.of(courierId);
        }
        return Optional.empty();
    }

    @Override
    public CompletableFuture<Void> processMealReady(OrderPreparedEvent mealReadyEvent) {
        CompletableFuture<Integer> future = CompletableFuture
                .supplyAsync(new MealAwaitingPickupSupplier(couriersAwaitingPickup, mealReadyEvent.readySince))
                .thenApplyAsync(ev -> {
                    if(null != ev) {
                        log.info("[EVENT] Order {} picked by Courier {} at {}",
                                mealReadyEvent.deliveryOrderId, ev.courierId, KitchenClock.formatted(ev.at));
                        recordMetrics(mealReadyEvent, ev);
                        return ev.courierId;
                    }
                    return null;
                });
        return future.thenApplyAsync(id -> {
            if(id != null) {
                courierFleet.release(id);
            } else {
                log.error("Error processing meal ready event: courierId is null");
            }
            return null;
        });
    }

    private void recordMetrics(OrderPreparedEvent mealReadyEvent, OrderPickedUpEvent orderPickedUpEvent) {
        foodWaitTimeStats.accept(orderPickedUpEvent.foodWaitTime);
        courierWaitTimeStats.accept(orderPickedUpEvent.courierWaitTime);
        log.info("[METRICS] Food wait time: {}ms id[{}], Courier wait time {}ms",
                orderPickedUpEvent.foodWaitTime,
                mealReadyEvent.deliveryOrderId,
                orderPickedUpEvent.courierWaitTime
                );
    }
}
