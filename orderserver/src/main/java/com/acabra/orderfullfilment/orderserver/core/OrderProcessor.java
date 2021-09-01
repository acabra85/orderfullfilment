package com.acabra.orderfullfilment.orderserver.core;

import com.acabra.orderfullfilment.orderserver.event.OrderReceivedEvent;

import java.util.concurrent.CompletableFuture;

public interface OrderProcessor {

    /**
     * Dispatches a courier and requests a meal cooking id to the kitchen reports if the order was accepted.
     * @param orderReceived An event indicating an order was received in the system
     * @return a handler to the completion state indicating whether the order was accepted. (e.g. if no couriers are
     *      available or the kitchen is not available)
     */
    CompletableFuture<Boolean> processOrder(OrderReceivedEvent orderReceived);
}
