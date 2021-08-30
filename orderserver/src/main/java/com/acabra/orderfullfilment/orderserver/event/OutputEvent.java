package com.acabra.orderfullfilment.orderserver.event;

public abstract class OutputEvent {
    public final EventType type;

    protected OutputEvent(EventType type) {
        this.type = type;
    }
}
