package com.acabra.orderfullfilment.orderserver.core;

public interface Extinguishable {

    /**
     * Cleans up the underlying resources by signaling through events to the handlers to finish their work
     */
    void shutDown();
}
