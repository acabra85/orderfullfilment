package com.acabra.orderfullfilment.orderserver.courier.event;

public class PickupEvent {
    public final int courierId;
    public final long arrivalTime;
    public final long eta;

    public PickupEvent(int courierId, long eta, long arrivalTime) {
        this.courierId = courierId;
        this.eta = eta;
        this.arrivalTime = arrivalTime;
    }
}
