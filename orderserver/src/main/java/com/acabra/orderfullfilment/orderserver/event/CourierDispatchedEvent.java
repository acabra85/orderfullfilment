package com.acabra.orderfullfilment.orderserver.event;

import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;

public class CourierDispatchedEvent extends OutputEvent{
    public final Integer courierId;
    public final DeliveryOrder order;
    public final long estimatedTravelTime;

    private CourierDispatchedEvent(long createdAt, DeliveryOrder order, Integer courierId, long estimatedTravelTime) {
        super(EventType.COURIER_DISPATCHED, createdAt);
        this.order = order;
        this.courierId = courierId;
        this.estimatedTravelTime = estimatedTravelTime;
    }

    public static CourierDispatchedEvent of(long now, DeliveryOrder order, Integer courierId, long estimatedTravelTime) {
        return new CourierDispatchedEvent(now, order, courierId, estimatedTravelTime);
    }
}
