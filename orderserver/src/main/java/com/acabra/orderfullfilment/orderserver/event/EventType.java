package com.acabra.orderfullfilment.orderserver.event;

import java.util.Comparator;

public enum EventType {
    ORDER_DELIVERED("order delivered", 1),
    ORDER_PICKED_UP("order picked up", 2),
    COURIER_DISPATCHED("courier dispatched", 3),
    COURIER_ARRIVED("courier arrived", Constants.SHARED_PRIORITY),
    ORDER_PREPARED("order prepared", Constants.SHARED_PRIORITY),
    ORDER_RECEIVED("order received", 5),
    NO_PENDING_ORDERS("no pending orders", 6); // the orders queue is empty all workers are waiting

    private static final Comparator<EventType> COMP = Comparator.comparing(a -> a.priority);
    public final String message;
    private final int priority;

    EventType(String message, int priority) {
        this.message = message;
        this.priority = priority;
    }

    public static Comparator<EventType> comparator() {
        return COMP;
    }

    private static class Constants {
        public static final int SHARED_PRIORITY = 4;
    }
}
