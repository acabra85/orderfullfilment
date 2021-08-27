package com.acabra.orderfullfilment.couriermodule.service;

import java.util.NoSuchElementException;

public interface CourierService {

    /**
     * Dispatches a courier to the kitchen in order for order pickup
     * @return id of the courier dispatched
     */
    int dispatch();

    /**
     * Releases a courier from the assignment, to get the status back to Available
     * @param courierId the id of the courier to release
     * @throws NoSuchElementException if the given id does not match an Assigned Courier
     */
    void release(int courierId) throws NoSuchElementException;
}
