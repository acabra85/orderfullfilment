package com.acabra.orderfullfilment.orderserver.event;

public class OrderPickedUpEvent extends OutputEvent {
    public final long courierWaitTime;
    public final long foodWaitTime;
    public final Integer courierId;
    public final Long mealOrderId;

    private OrderPickedUpEvent(long createdAt, long courierWaitTime, long foodWaitTime, Integer courierId, long mealOrderId) {
        super(EventType.ORDER_PICKED_UP, createdAt);
        this.courierWaitTime = courierWaitTime;
        this.foodWaitTime = foodWaitTime;
        this.courierId = courierId;
        this.mealOrderId = mealOrderId;
    }

    public static OrderPickedUpEvent of(long now, CourierArrivedEvent courierArrivedEvent, long orderReadySince, long mealOrderId) {
        long courierWaitTime = now - courierArrivedEvent.createdAt;
        long foodWaitTime = now - orderReadySince;
        return new OrderPickedUpEvent(now, courierWaitTime, foodWaitTime, courierArrivedEvent.courierId, mealOrderId);
    }
}
