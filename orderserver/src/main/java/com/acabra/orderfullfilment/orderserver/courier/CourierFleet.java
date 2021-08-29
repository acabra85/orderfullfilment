package com.acabra.orderfullfilment.orderserver.courier;

import com.acabra.orderfullfilment.orderserver.courier.event.CourierReadyForPickupEvent;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;

import java.util.NoSuchElementException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

public interface CourierFleet {

    /**
     *
     * Dispatches a courier to the kitchen for order pickup, it does
     * @param order an optional order object representing an order that started the
     * @return id of the courier dispatched or null if no drivers are available
     */
    Integer dispatch(DeliveryOrder order);

    /**
     * Releases a courier from the assignment, to get the status back to Available
     * @param courierId the id of the courier to release
     * @throws NoSuchElementException if the given id does not match an Assigned Courier
     */
    void release(Integer courierId) throws NoSuchElementException;

    /**
     * The total amount of couriers registered
     * @return size
     */
    int fleetSize();

    /**
     * The total amount of couriers available from the fleetSize
     * @return total available
     */
    int availableCouriers();

    /**
     * Registration of the notification queue to be used to publish the Ready for Pickup event
     */
    void registerNotificationQueue(BlockingDeque<CourierReadyForPickupEvent> deque);
}
