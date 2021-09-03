package com.acabra.orderfullfilment.orderserver.kitchen;

import com.acabra.orderfullfilment.orderserver.event.OrderPreparedEvent;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class Chef {
    private final long mealOrderId;
    private final DeliveryOrder order;

    private Chef(long mealReservationId, DeliveryOrder order) {
        this.mealOrderId = mealReservationId;
        this.order = order;
    }

    public static Chef of(long mealOrderId, DeliveryOrder order) {
        return new Chef(mealOrderId, order);
    }

    public CompletableFuture<OrderPreparedEvent> prepareMeal() {
        return CompletableFuture.supplyAsync(() -> {
            long now = KitchenClock.now();
            OrderPreparedEvent orderPreparedEvent = OrderPreparedEvent.of(Chef.this.mealOrderId, Chef.this.order.id, now);
                log.info("[EVENT] order prepared: mealId{}, orderId[{}] at {}", orderPreparedEvent.mealOrderId,
                        orderPreparedEvent.deliveryOrderId, KitchenClock.formatted(orderPreparedEvent.createdAt));
                return orderPreparedEvent;
                },
            CompletableFuture.delayedExecutor(order.prepTime, TimeUnit.MILLISECONDS));
    }
}