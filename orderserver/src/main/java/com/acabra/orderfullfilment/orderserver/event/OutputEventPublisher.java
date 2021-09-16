package com.acabra.orderfullfilment.orderserver.event;

import org.slf4j.Logger;

import java.util.Deque;
import java.util.concurrent.atomic.AtomicReference;

public interface OutputEventPublisher {

    /**
     * Implementations of this interface allow deferred registration to a blocking queue of events
     * @param deque the blocking queue that will be used for publishing
     */
    void registerNotificationDeque(Deque<OutputEvent> deque);

    /**
     * A reference to the pubDeque to allow publication of events
     * @return an atomic reference to the pudDeque
     */
    AtomicReference<Deque<OutputEvent>> getPubDeque();

    /**
     * Provides the logger for the implementation
     * @return a logger object
     */
    Logger log();

    default boolean publish(OutputEvent event) {
        if(null != getPubDeque().get()) {
            try {
                getPubDeque().get().offer(event);
                return true;
            } catch (Throwable e) {
                log().error("Unable to publish event: {}", e.getMessage(), e);
            }
        }
        return false;
    }

}
