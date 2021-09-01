package com.acabra.orderfullfilment.orderproducer.dispatch;

import com.acabra.orderfullfilment.orderproducer.dto.DeliveryOrderRequest;

import java.util.List;

public interface PeriodicOrderDispatcherClient {
    /**
     * Given a list of order requests dispatches them 2 every second to the defined .
     * @param orders list of orders
     * @param orderFrequency orders per second
     */
    void dispatchEverySecond(int orderFrequency, List<DeliveryOrderRequest> orders);
}
