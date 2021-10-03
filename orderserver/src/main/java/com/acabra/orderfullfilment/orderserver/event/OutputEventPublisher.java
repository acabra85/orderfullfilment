package com.acabra.orderfullfilment.orderserver.event;

import java.util.Queue;

public interface OutputEventPublisher {

    /**
     * Implementations of this interface allow deferred registration to a blocking queue of events
     * @param deque the blocking queue that will be used for publishing
     */
    void registerNotificationDeque(Queue<OutputEvent> deque);

    /**
     * A reference to the pubDeque to allow publication of events
     * @return an atomic reference to the pudDeque
     */
    Queue<OutputEvent> getPubDeque();

    /**
     * Provides the logger for the implementation
     * @param msg message to display
     * @param e throwable causing the error
     */
    void logError(String msg, Throwable e);

    default boolean publish(OutputEvent event) {
        Queue<OutputEvent> pubDeque = getPubDeque();
        if(null != pubDeque) {
            try {
                pubDeque.offer(event);
                return true;
            } catch (Throwable e) {
                logError("Unable to publish event: " + e.getMessage(), e);
            }
        }
        return false;
    }

}
