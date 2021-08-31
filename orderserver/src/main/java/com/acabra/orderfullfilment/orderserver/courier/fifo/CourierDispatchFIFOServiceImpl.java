package com.acabra.orderfullfilment.orderserver.courier.fifo;

import com.acabra.orderfullfilment.orderserver.courier.CourierDispatchService;
import com.acabra.orderfullfilment.orderserver.courier.CourierFleet;
import com.acabra.orderfullfilment.orderserver.event.OrderPickedUpEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import com.acabra.orderfullfilment.orderserver.event.OrderPreparedEvent;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "orderserver", name = "strategy", havingValue = "fifo")
public class CourierDispatchFIFOServiceImpl implements CourierDispatchService {

    private final BlockingDeque<OutputEvent> internalNotificationQueue;
    private final CourierFleet courierFleet;
    private final AtomicReference<BlockingQueue<OutputEvent>> publicNotificationQueue = new AtomicReference<>();

    @Autowired
    public CourierDispatchFIFOServiceImpl(CourierFleet courierFleet) {
        final BlockingDeque<OutputEvent> deque = new LinkedBlockingDeque<>();
        this.courierFleet = courierFleet;
        this.internalNotificationQueue = deque;
        this.courierFleet.registerNotificationDeque(deque);
    }

    public CourierDispatchFIFOServiceImpl(CourierFleet courierFleet,
                                          BlockingDeque<OutputEvent> blockingDeque) {
        this.courierFleet = courierFleet;
        this.internalNotificationQueue = blockingDeque;
        this.courierFleet.registerNotificationDeque(blockingDeque);
    }

    @Override
    public Optional<Integer> dispatchRequest(DeliveryOrder order) {
        Integer courierId = this.courierFleet.dispatch(order).courierId;
        if(null != courierId) {
            return Optional.of(courierId);
        }
        return Optional.empty();
    }

    @Override
    public CompletableFuture<Void> processMealReady(OrderPreparedEvent orderPreparedEvent) {
        CompletableFuture<Integer> future = CompletableFuture
                .supplyAsync(MealAwaitingPickupSupplier.of(internalNotificationQueue, orderPreparedEvent))
                .thenApplyAsync(orderPickedUpEvent -> {
                    if(null != orderPickedUpEvent) {
                        log.info("[EVENT] order picked up: orderId[{}] courierId[{}] at {}",
                                orderPreparedEvent.deliveryOrderId, orderPickedUpEvent.courierId,
                                KitchenClock.formatted(orderPickedUpEvent.createdAt));
                                CompletableFuture.runAsync(() -> publishOrderPickedUpEvent(orderPickedUpEvent));
                        return orderPickedUpEvent.courierId;
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

    private void publishOrderPickedUpEvent(OrderPickedUpEvent orderPickedUpEvent) {
        if(null != this.publicNotificationQueue.get()) {
            try {
                this.publicNotificationQueue.get().put(orderPickedUpEvent);
            } catch (InterruptedException e) {
                log.error("Unable to publish order received event");
            }
        }
    }

    @Override
    public void registerNotificationDeque(BlockingDeque<OutputEvent> deque) {
        this.publicNotificationQueue.updateAndGet(old -> deque);
    }
}
