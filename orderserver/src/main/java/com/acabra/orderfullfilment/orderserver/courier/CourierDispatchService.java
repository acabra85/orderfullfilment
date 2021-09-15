package com.acabra.orderfullfilment.orderserver.courier;

import com.acabra.orderfullfilment.orderserver.event.*;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;

import java.util.Optional;

public interface CourierDispatchService extends OutputEventPublisher {

    /**
     * Dispatches an available courier for order pickup
     * @param order the order that initiated the dispatch (please not the dispatched courier might not necessarily
     *              pick up that order.
     * @param reservationId the prepare reservation id given by the kitchen
     * @return an optional value with the dispatched courierId or empty if no couriers are available
     */
    Optional<Integer> dispatchRequest(DeliveryOrder order, long reservationId);

    /**
     * According to the strategy defined allows the order to be picked up by an awaiting courier or else to await
     * for pickup.
     * @param mealReadyEvent the details of the meal prepared
     * @return a completable future of boolean indicating if the event was accepted or not
     */
    boolean processOrderPrepared(OrderPreparedEvent mealReadyEvent);

    /**
     * Dispatch service releases the assignment from the courier
     * @param orderDeliveredEvent event
     */
     void processOrderDelivered(OrderDeliveredEvent orderDeliveredEvent);

    /**
     * According to the defined strategy matches the courier with the corresponding order or to await for an order to
     * arrive
     * @param courierArrivedEvent event notification courrier arrived
     * @return a handle to allow callers take action upon completion, indicates whether the event was accepted by
     * the service
     */
    boolean processCourierArrived(CourierArrivedEvent courierArrivedEvent);

    /**
     * Notifies the courier service of the courierDispatched event, this allows
     * @param courierDispatchedEvent event indicating a courier was dispatched
     */
    void processCourierDispatchedEvent(CourierDispatchedEvent courierDispatchedEvent);

    /**
     * Request the shutdown of operations, (e.g. attempts to dispatch couriers)
     */
    void shutdown();
}
