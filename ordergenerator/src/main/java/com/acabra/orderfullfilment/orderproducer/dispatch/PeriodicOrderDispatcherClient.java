package com.acabra.orderfullfilment.orderproducer.dispatch;

import com.acabra.orderfullfilment.orderproducer.dto.DeliveryOrderRequestDTO;
import com.acabra.orderfullfilment.orderproducer.dto.OrderDispatcherStatusPOJO;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface PeriodicOrderDispatcherClient {
    /**
     * Given a list of order requests dispatches them 2 every second to the defined .
     * @param orders list of orders
     */
    void dispatchTwoOrdersPerSecond(List<DeliveryOrderRequestDTO> orders);

    /**
     * @return CompletableFuture for the caller to track the completion of the task, the future
     *         will contain the result of the dispatch of the orders.
     */
    CompletableFuture<OrderDispatcherStatusPOJO> getCompletionFuture();
}
