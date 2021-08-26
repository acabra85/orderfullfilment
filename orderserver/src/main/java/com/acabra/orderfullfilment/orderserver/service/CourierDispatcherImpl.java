package com.acabra.orderfullfilment.orderserver.service;

import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CourierDispatcherImpl implements CourierDispatcher {

    private final CourierDispatchStrategy courierDispatcher;
    private final static Logger logger = LoggerFactory.getLogger(CourierDispatcherImpl.class);
    private final KitchenService kitchen;

    public CourierDispatcherImpl(CourierDispatchStrategy dispatchStrategy, KitchenService kitchen) {
        this.courierDispatcher = dispatchStrategy;
        this.kitchen = kitchen;
    }

    @Override
    public void dispatchOrder(DeliveryOrder order) {
        logger.info("dispatching order {}:" + order.id);
        courierDispatcher.dispatchCourier(order);
        kitchen.cookOrder(order);
    }
}
