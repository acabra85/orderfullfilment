package com.acabra.orderfullfilment.orderserver.event;

public class OrderPickedUpEvent extends OutputEvent {
    public final long courierWaitTime;
    public final long foodWaitTime;
    public final Integer courierId;
    public final long at;

    public OrderPickedUpEvent(long now, long courierWaitTime, long foodWaitTime, Integer courierId) {
        super(EventType.ORDER_PICKED_UP);
        this.at = now;
        this.courierWaitTime = courierWaitTime;
        this.foodWaitTime = foodWaitTime;
        this.courierId = courierId;
    }
}
