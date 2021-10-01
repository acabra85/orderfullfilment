package com.acabra.orderfullfilment.orderserver.event;

public class CourierArrivedEvent extends OutputEvent {
    public final Integer courierId;
    public final long ett;

    private CourierArrivedEvent(Integer courierId, long ett, long createdAt) {
        super(EventType.COURIER_ARRIVED, createdAt);
        this.courierId = courierId;
        this.ett = ett;
    }

    public static CourierArrivedEvent of(int courierId, long ett, long now) {
        return new CourierArrivedEvent(courierId, ett, now);
    }
}
