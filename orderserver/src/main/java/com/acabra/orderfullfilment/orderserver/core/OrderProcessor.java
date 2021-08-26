package com.acabra.orderfullfilment.orderserver.core;

import com.acabra.orderfullfilment.orderserver.dto.DeliveryOrderRequest;

public interface OrderProcessor {
    /**
     * Request a dispatch of a courier
     * Request meal cooking to the kitchen
     * @param orderRequest an order request
     */
    void processOrder(DeliveryOrderRequest orderRequest);
}
