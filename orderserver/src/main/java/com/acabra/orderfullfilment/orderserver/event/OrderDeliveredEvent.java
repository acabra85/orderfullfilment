package com.acabra.orderfullfilment.orderserver.event;

public class OrderDeliveredEvent extends OutputEvent{
    public final Long mealOrderId;
    public final Integer courierId;
    private OrderDeliveredEvent(long createdAt, Long mealOrderId, Integer courierId) {
        super(EventType.ORDER_DELIVERED, createdAt);
        this.mealOrderId = mealOrderId;
        this.courierId = courierId;
    }

    public static OrderDeliveredEvent of(OrderPickedUpEvent orderPickedUpEvent) {
        return new OrderDeliveredEvent(orderPickedUpEvent.createdAt, orderPickedUpEvent.mealOrderId,
                orderPickedUpEvent.courierId);
    }
}
