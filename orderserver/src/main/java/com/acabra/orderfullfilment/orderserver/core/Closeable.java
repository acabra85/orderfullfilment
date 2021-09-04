package com.acabra.orderfullfilment.orderserver.core;

public interface Closeable {
    /**
     * Cleans up the underlying resources by signaling termination events to the handlers.
     */
    void close();
}
