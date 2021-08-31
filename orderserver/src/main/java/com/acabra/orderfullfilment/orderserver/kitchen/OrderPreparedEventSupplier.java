package com.acabra.orderfullfilment.orderserver.kitchen;

import com.acabra.orderfullfilment.orderserver.event.OrderPreparedEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

@Slf4j
public class OrderPreparedEventSupplier implements Supplier<OrderPreparedEvent> {
    private final long mealOrderId;
    private final String deliveryOrderId;

    private OrderPreparedEventSupplier(long mealReservationId, String deliveryOrderId) {
        this.mealOrderId = mealReservationId;
        this.deliveryOrderId = deliveryOrderId;
    }

    public static OrderPreparedEventSupplier of(long mealOrderId, String id) {
        return new OrderPreparedEventSupplier(mealOrderId, id);
    }

    @Override
    public OrderPreparedEvent get() {
        OrderPreparedEvent orderPreparedEvent = OrderPreparedEvent.of(this.mealOrderId, this.deliveryOrderId,
                KitchenClock.now());
        log.info("[EVENT] order prepared: mealId{}, orderId[{}] at {}", orderPreparedEvent.mealOrderId,
                orderPreparedEvent.deliveryOrderId, KitchenClock.formatted(orderPreparedEvent.createdAt));
        return orderPreparedEvent;
    }
}