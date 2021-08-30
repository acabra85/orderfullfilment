package com.acabra.orderfullfilment.orderserver.event;

public class OrderPreparedEvent extends OutputEvent{
    public final long mealOrderId;
    public final long readySince;
    public final String deliveryOrderId;

    private OrderPreparedEvent(long mealOrderId, String deliveryOrderId, long readySince) {
        super(EventType.ORDER_PREPARED);
        this.mealOrderId = mealOrderId;
        this.readySince = readySince;
        this.deliveryOrderId = deliveryOrderId;
    }

    public static OrderPreparedEvent of(long mealOrderId, String deliveryOrderId, long readySince) {
        return new OrderPreparedEvent(mealOrderId, deliveryOrderId, readySince);
    }
}