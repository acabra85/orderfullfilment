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

    public static OrderPickedUpEvent of(long now, CourierArrivedEvent courierEvt, OrderPreparedEvent orderEvt,
                                        boolean completedCourier, long waitTime) {
        long courierWaitTime = completedCourier ? 0 : waitTime;
        long foodWaitTime = !completedCourier ? 0 : waitTime;
        return new OrderPickedUpEvent(now, courierWaitTime, foodWaitTime, courierEvt.courierId,
                orderEvt.kitchenReservationId);
    }
}
