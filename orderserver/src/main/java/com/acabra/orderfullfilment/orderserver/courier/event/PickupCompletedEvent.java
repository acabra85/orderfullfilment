package com.acabra.orderfullfilment.orderserver.courier.event;

public class PickupCompletedEvent {
    public final long courierWaitTime;
    public final long foodWaitTime;
    public final int courierId;
    public final long at;

    public PickupCompletedEvent(long now, long courierWaitTime, long foodWaitTime, int courierId) {
        this.at = now;
        this.courierWaitTime = courierWaitTime;
        this.foodWaitTime = foodWaitTime;
        this.courierId = courierId;
    }
}