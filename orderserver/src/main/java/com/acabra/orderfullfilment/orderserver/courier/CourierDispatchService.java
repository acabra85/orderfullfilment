package com.acabra.orderfullfilment.orderserver.courier;

import com.acabra.orderfullfilment.orderserver.event.OrderPreparedEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;

import java.util.Optional;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;

public interface CourierDispatchService {

    /**
     * Dispatches a courier to pick up the given order
     * @return id of the courier
     */
    Optional<Integer> dispatchRequest(DeliveryOrder order);

    /**
     * According to the strategy defined allows the order to be picked up by an awaiting courier or else to await
     * for pickup.
     * @param mealReadyEvent the details of the meal prepared
     * @return a completable future allowing callers to take additional actions upon completion (if needed)
     */
    CompletableFuture<Void> processMealReady(OrderPreparedEvent mealReadyEvent);

    /**
     * Registration of the notification queue to be used to publish the Ready for Pickup event
     */
    void registerNotificationDeque(BlockingDeque<OutputEvent> deque);
}
