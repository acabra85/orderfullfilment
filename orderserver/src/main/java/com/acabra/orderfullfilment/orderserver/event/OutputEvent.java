package com.acabra.orderfullfilment.orderserver.event;

public abstract class OutputEvent {
    public final EventType type;
    public final long createdAt;

    protected OutputEvent(EventType type, long createdAt) {
        this.type = type;
        this.createdAt = createdAt;
    }

    public EventType getType() {
        return type;
    }
}
