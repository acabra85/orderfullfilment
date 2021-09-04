package com.acabra.orderfullfilment.orderserver.event;

public enum EventType {
    ORDER_RECEIVED("order received"),
    COURIER_DISPATCHED("courier dispatched"),
    ORDER_PREPARED("order prepared"),
    COURIER_ARRIVED("courier arrived"),
    ORDER_PICKED_UP("order picked up"),
    ORDER_DELIVERED("order delivered"),
    SHUT_DOWN_REQUEST("shut down request"), // a request to shut down the workers
    NO_PENDING_ORDERS("no pending orders"); // the orders queue is empty all workers are waiting

    public final String message;

    EventType(String message) {
        this.message = message;
    }
}
