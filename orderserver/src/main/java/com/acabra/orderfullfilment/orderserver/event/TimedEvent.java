package com.acabra.orderfullfilment.orderserver.event;

public class TimedEvent<T extends OutputEvent> {
    private final T event;
    private final long startedAt;

    public TimedEvent(T event) {
        this.event = event;
        this.startedAt = System.currentTimeMillis();
    }

    public long stop() {
        return System.currentTimeMillis() - startedAt;
    }

    public T getEvent() {
        return this.event;
    }
}
