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

    public static OrderPickedUpEvent of(CourierArrivedEvent courierEvt, OrderPreparedEvent orderEvt) {
        long diffTime = courierEvt.createdAt - orderEvt.createdAt;
        if(diffTime > 0) {
            return new OrderPickedUpEvent(courierEvt.createdAt, 0, diffTime, courierEvt.courierId,
                    orderEvt.kitchenReservationId);
        }
        return new OrderPickedUpEvent(orderEvt.createdAt, Math.abs(diffTime), 0, courierEvt.courierId,
                orderEvt.kitchenReservationId);
    }
}
