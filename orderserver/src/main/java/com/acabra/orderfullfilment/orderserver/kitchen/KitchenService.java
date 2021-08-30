package com.acabra.orderfullfilment.orderserver.kitchen;

import com.acabra.orderfullfilment.orderserver.event.OrderPreparedEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;

public interface KitchenService {

    /**
     * Assigns the current order to an internal cookId, this operation is reversible by calling @cancelCookReservation
     * This allows the caller to ensure a delivery driver is available before the kitchen starts meal preparation
     * @param order the delivery order
     * @return id of the reservation
     */
    long orderCookReservationId(DeliveryOrder order);

    /**
     * Cancels the reservation order to cook a meal
     * @param mealReservationId id of the reservation
     * @return true if reservation existed or false otherwise
     */
    boolean cancelCookReservation(long mealReservationId);

    /**
     * Instructs the kitchen to start meal preparation as a courier has been reserved to handle the order
     * @param cookReservationId the reservation id provided by calling @orderCookReservationId
     * @return a future handle to determine if the notification meal ready was published successfully.
     */
    CompletableFuture<Boolean> prepareMeal(long cookReservationId);

    /**
     * Register a queue for notification of meal ready for pickup
     * @param queue a blocking queue
     */
    void registerMealNotificationReadyQueue(BlockingDeque<OutputEvent> queue);

    /**
     * Reports whether the kitchen is preparing meals or not
     * @return true if no meals are being prepared.
     */
    boolean isKitchenIdle();
}
