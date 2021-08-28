package com.acabra.orderfullfilment.orderserver.kitchen;

import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;

public interface KitchenService {

    /**
     * Assigns the current order to an internal cook id, this operation is reversible by calling @cancelCookReservation
     * This allows the caller to ensure a delivery driver is available before the kitchen starts meal preparation
     * @param order
     * @return
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
}
