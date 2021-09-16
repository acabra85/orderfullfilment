package com.acabra.orderfullfilment.orderserver.event;

public class OrderPreparedEvent extends OutputEvent {
    public final long kitchenReservationId;
    public final String deliveryOrderId;

    private OrderPreparedEvent(long kitchenReservationId, String deliveryOrderId, long createdAt) {
        super(EventType.ORDER_PREPARED, createdAt);
        this.kitchenReservationId = kitchenReservationId;
        this.deliveryOrderId = deliveryOrderId;
    }

    public static OrderPreparedEvent of(long kitchenReservationId, String deliveryOrderId, long readySince) {
        return new OrderPreparedEvent(kitchenReservationId, deliveryOrderId, readySince);
    }
}