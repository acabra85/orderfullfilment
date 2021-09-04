package com.acabra.orderfullfilment.orderserver.event;

import java.util.Deque;

public interface OutputEventPublisher {

    /**
     * Implementations of this interface allow deferred registration to a blocking queue of events
     * @param deque the blocking queue that will be used for publishing
     */
    void registerNotificationDeque(Deque<OutputEvent> deque);
}
