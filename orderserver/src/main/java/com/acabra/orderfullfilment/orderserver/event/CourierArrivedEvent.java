package com.acabra.orderfullfilment.orderserver.event;

public class CourierArrivedEvent extends OutputEvent {
    public final Integer courierId;
    public final long eta;

    private CourierArrivedEvent(Integer courierId, long eta, long createdAt) {
        super(EventType.COURIER_ARRIVED, createdAt);
        this.courierId = courierId;
        this.eta = eta;
    }

    public static CourierArrivedEvent of(int courierId, long eta, long now) {
        return new CourierArrivedEvent(courierId, eta, now);
    }
}
