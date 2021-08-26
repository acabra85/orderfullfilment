package com.acabra.orderfullfilment.orderserver.service;

import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;

public interface CourierDispatcher {
    void dispatchOrder(DeliveryOrder order);
}
