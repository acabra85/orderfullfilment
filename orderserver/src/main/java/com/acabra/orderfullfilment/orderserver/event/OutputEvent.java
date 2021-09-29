package com.acabra.orderfullfilment.orderserver.event;

import java.util.Comparator;

public abstract class OutputEvent implements Comparable<OutputEvent> {
    public static final Comparator<EventType> EVENT_TYPE_COMPARATOR = EventType.comparator();
    public final EventType type;
    public final long createdAt;

    protected OutputEvent(EventType type, long createdAt) {
        this.type = type;
        this.createdAt = createdAt;
    }

    public EventType getType() {
        return type;
    }

    @Override
    public int compareTo(OutputEvent o) {
        int compare = EVENT_TYPE_COMPARATOR.compare(type, o.type);
        return 0 != compare ? compare : Long.compare(createdAt, o.createdAt);
    }
}
