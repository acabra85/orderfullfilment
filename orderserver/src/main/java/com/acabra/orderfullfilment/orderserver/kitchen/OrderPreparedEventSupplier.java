package com.acabra.orderfullfilment.orderserver.kitchen;

import com.acabra.orderfullfilment.orderserver.event.OrderPreparedEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.function.Supplier;

@Slf4j
public class OrderPreparedEventSupplier implements Supplier<OrderPreparedEvent> {
    private final long mealOrderId;
    private final String deliveryOrderId;

    public OrderPreparedEventSupplier(long mealReservationId, String deliveryOrderId) {
        this.mealOrderId = mealReservationId;
        this.deliveryOrderId = deliveryOrderId;
    }

    @Override
    public OrderPreparedEvent get() {
        OrderPreparedEvent readyForPickup = OrderPreparedEvent.of(this.mealOrderId,
                this.deliveryOrderId,
                KitchenClock.now());
        log.info("[EVENT] Order prepared: id[{}] Ready for pickup at: {}", deliveryOrderId,
                KitchenClock.formatted(readyForPickup.readySince));
        return readyForPickup;
    }
}