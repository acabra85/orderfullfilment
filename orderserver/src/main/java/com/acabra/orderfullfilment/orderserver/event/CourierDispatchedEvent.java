package com.acabra.orderfullfilment.orderserver.event;

import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;

public class CourierDispatchedEvent extends OutputEvent{
    public final Integer courierId;
    public final DeliveryOrder order;

    private CourierDispatchedEvent(long createdAt, DeliveryOrder order, Integer courierId) {
        super(EventType.COURIER_DISPATCHED, createdAt);
        this.order = order;
        this.courierId = courierId;
    }

    public static CourierDispatchedEvent of(long now, DeliveryOrder order, Integer courierId) {
        return new CourierDispatchedEvent(now, order, courierId);
    }
}
