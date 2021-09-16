package com.acabra.orderfullfilment.orderserver.kitchen;

import com.acabra.orderfullfilment.orderserver.event.OutputEventPublisher;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;

import java.util.concurrent.CompletableFuture;

public interface KitchenService extends OutputEventPublisher {

    /**
     * Assigns the current order to an internal cookId, this operation is reversible by calling @cancelCookReservation
     * This allows the caller to ensure a delivery driver is available before the kitchen starts meal preparation
     * @param order the delivery order
     * @return id of the reservation
     */
    long provideReservationId(DeliveryOrder order);

    /**
     * Cancels the reservation order to cook a meal
     * @param kitchenReservationId id of the reservation
     * @return true if reservation existed or false otherwise
     */
    boolean cancelCookReservation(long kitchenReservationId);

    /**
     * Instructs the kitchen to start meal preparation as a courier has been reserved to handle the order
     * @param kitchenReservationId the reservation id provided by calling @orderCookReservationId
     * @return a future handle to determine if the notification meal ready was published successfully.
     */
    CompletableFuture<Boolean> prepareMeal(long kitchenReservationId);

    /**
     * Reports whether the kitchen is preparing meals or not
     * @return true if no meals are being prepared.
     */
    boolean isKitchenIdle();

    /**
     * Request the shutdown of operations (e.g. such as cooking meals)
     */
    void shutdown();
}
