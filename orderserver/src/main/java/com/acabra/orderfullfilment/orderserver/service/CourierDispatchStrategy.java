package com.acabra.orderfullfilment.orderserver.service;

import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;

public interface CourierDispatchStrategy {

    /**
     * This functionality matches an order to a courier and returns a
     * @param order
     * @return MatchResponse object declaring the id of the courier and the
     */
    int dispatchCourier(DeliveryOrder order);
}
