package com.acabra.orderfullfilment.couriermodule.service;

public interface CourierService {
    /**
     * Looks for available couriers and assigns one to the given order
     * This service always succeeds, in case no drivers are available a new one is added to the system
     * @param orderId the orderId of the meal delivery order
     * @return id of the driver matched to pick-up the order at the restaurant
     */
    int matchToOrder(String orderId);

    /**
     * Dispatches a courier to the kitchen in order to pick up the next available order
     * @return id of the dispatch courier
     */
    int dispatch();
}
