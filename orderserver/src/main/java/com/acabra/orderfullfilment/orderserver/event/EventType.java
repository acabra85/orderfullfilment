package com.acabra.orderfullfilment.orderserver.event;

public enum EventType {
    ORDER_RECEIVED("order received"),
    ORDER_PREPARED("order prepared"),
    COURIER_DISPATCHED("courier dispatched"),
    COURIER_ARRIVED("courier arrived"),
    ORDER_PICKED_UP("order picked up"),
    ORDER_DELIVERED("order delivered"),
    ALL_ORDERS_HANDLED("all orders handled");

    public final String message;

    EventType(String message) {
        this.message = message;
    }
}
