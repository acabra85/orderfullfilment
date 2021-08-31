package com.acabra.orderfullfilment.orderserver.event;

public class OrderPreparedEvent extends OutputEvent {
    public final long mealOrderId;
    public final String deliveryOrderId;

    private OrderPreparedEvent(long mealOrderId, String deliveryOrderId, long createdAt) {
        super(EventType.ORDER_PREPARED, createdAt);
        this.mealOrderId = mealOrderId;
        this.deliveryOrderId = deliveryOrderId;
    }

    public static OrderPreparedEvent of(long mealOrderId, String deliveryOrderId, long readySince) {
        return new OrderPreparedEvent(mealOrderId, deliveryOrderId, readySince);
    }
}