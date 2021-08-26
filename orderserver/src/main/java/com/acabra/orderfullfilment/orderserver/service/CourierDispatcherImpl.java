package com.acabra.orderfullfilment.orderserver.service;

import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CourierDispatcherImpl implements CourierDispatcher {

    private final CourierDispatchStrategy dispatcher;
    private final static Logger logger = LoggerFactory.getLogger(CourierDispatcherImpl.class);

    public CourierDispatcherImpl(CourierDispatchStrategy dispatchStrategy) {
        this.dispatcher = dispatchStrategy;
    }

    @Override
    public void dispatchOrder(DeliveryOrder order) {
        logger.info("dispatching order {}:" + order.id);
        dispatcher.dispatchCourier(order);
    }
}
