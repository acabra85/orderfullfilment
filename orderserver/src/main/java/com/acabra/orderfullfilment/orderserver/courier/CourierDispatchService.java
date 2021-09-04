package com.acabra.orderfullfilment.orderserver.courier;

import com.acabra.orderfullfilment.orderserver.event.CourierArrivedEvent;
import com.acabra.orderfullfilment.orderserver.event.OrderDeliveredEvent;
import com.acabra.orderfullfilment.orderserver.event.OrderPreparedEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEventPublisher;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface CourierDispatchService extends OutputEventPublisher {

    /**
     * Dispatches a courier to pick up the given order
     * @return id of the courier
     */
    Optional<Integer> dispatchRequest(DeliveryOrder order);

    /**
     * According to the strategy defined allows the order to be picked up by an awaiting courier or else to await
     * for pickup.
     * @param mealReadyEvent the details of the meal prepared
     * @return a completable future of boolean indicating if the event was accepted or not
     */
    CompletableFuture<Boolean> processOrderPrepared(OrderPreparedEvent mealReadyEvent);

    /**
     * Dispatch service releases the assignment from the courier
     * @param orderDeliveredEvent event
     * @return a handle to allow callers to take action upon completion, the handle can complete with exceptions if the
     *         courierId is not valid
     */
    CompletableFuture<Void> processOrderDelivered(OrderDeliveredEvent orderDeliveredEvent);

    /**
     * According to the defined strategy matches the courier with the corresponding order or to await for an order to
     * arrive
     * @param courierArrivedEvent event notification courrier arrived
     * @return a handle to allow callers take action upon completion, indicates whether the event was accepted by
     * the service
     */
    CompletableFuture<Boolean> processCourierArrived(CourierArrivedEvent courierArrivedEvent);
}
