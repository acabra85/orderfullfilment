package com.acabra.orderfullfilment.orderserver.event;

public enum EventType {
    ORDER_RECEIVED("order received"),
    ORDER_PREPARED("order prepared"),
    COURIER_DISPATCHED("courier dispatched"),
    COURIER_ARRIVED("courier arrived"),
    ORDER_PICKED_UP("order picked up"),
    ORDER_DELIVERED("order delivered");

    public final String message;

    EventType(String message) {
        this.message = message;
    }
}
