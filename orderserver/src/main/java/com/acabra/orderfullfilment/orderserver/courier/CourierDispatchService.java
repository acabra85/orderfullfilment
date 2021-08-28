package com.acabra.orderfullfilment.orderserver.courier;

import com.acabra.orderfullfilment.orderserver.courier.event.CourierReadyForPickupEvent;
import com.acabra.orderfullfilment.orderserver.kitchen.event.MealReadyForPickupEvent;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface CourierDispatchService {

    /**
     * Dispatches a courier to pick up the given order
     * @return id of the courier
     */
    Optional<Integer> dispatchRequest(DeliveryOrder order);

    /**
     * According to the strategy defined, allows a courier to pickup an order if is ready or to wait in line
     * @param pickupEvent the details of the courier
     * @return true if the event is accepted for processing
     */
    boolean processCourierArrival(CourierReadyForPickupEvent pickupEvent);

    /**
     * According to the strategy defined allows the order to be picked up by an awaiting courier or else to await
     * for pickup.
     * @param mealReadyEvent the details of the meal prepared
     * @return a completable future allowing callers to take additional actions upon completion (if needed)
     */
    CompletableFuture<Void> processMealReady(MealReadyForPickupEvent mealReadyEvent);

}
