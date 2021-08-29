package com.acabra.orderfullfilment.orderserver.courier.event;

public class CourierReadyForPickupEvent {
    public final Integer courierId;
    public final long arrivalTime;
    public final long eta;

    public CourierReadyForPickupEvent(Integer courierId, long eta, long arrivalTime) {
        this.courierId = courierId;
        this.eta = eta;
        this.arrivalTime = arrivalTime;
    }
}
