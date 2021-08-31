package com.acabra.orderfullfilment.orderserver.core;

import com.acabra.orderfullfilment.orderserver.event.OrderReceivedEvent;

public interface OrderProcessor {
    /**
     * Request a dispatch of a courier
     * Request meal cooking to the kitchen
     * @param orderReceivedEvent an order received event
     */
    void processOrder(OrderReceivedEvent orderReceived);
}
