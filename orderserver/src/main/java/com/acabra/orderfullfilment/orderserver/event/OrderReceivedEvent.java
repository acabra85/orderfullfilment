package com.acabra.orderfullfilment.orderserver.event;

import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;

import java.util.Objects;

public class OrderReceivedEvent extends OutputEvent {
    public final DeliveryOrder order;

    private OrderReceivedEvent(long createdAt, DeliveryOrder order) {
        super(EventType.ORDER_RECEIVED, createdAt);
        this.order = order;
    }

    public static OrderReceivedEvent of(long createdAt, DeliveryOrder order) {
        return new OrderReceivedEvent(createdAt, Objects.requireNonNull(order));
    }
}
