package com.acabra.orderfullfilment.orderserver.kitchen;

import com.acabra.orderfullfilment.orderserver.kitchen.event.MealReadyForPickupEvent;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

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
     */
    void cancelCookReservation(long mealReservationId);

    /**
     * Instructs the kitchen to start meal preparation as a courier has been reserved to handle the order
     * @param cookReservationId the reservation id provided by calling @orderCookReservationId
     */
    void prepareMeal(long cookReservationId);

    /**
     *
     * @param queue
     */
    void registerMealNotificationReadyQueue(BlockingDeque<MealReadyForPickupEvent> queue);
}
