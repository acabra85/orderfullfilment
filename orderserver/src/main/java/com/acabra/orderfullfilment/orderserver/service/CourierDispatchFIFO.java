package com.acabra.orderfullfilment.orderserver.service;

import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "dispatch", name = "strategy", havingValue = "fifo")
public class CourierDispatchFIFO implements CourierDispatchStrategy {
    private final Logger logger = LoggerFactory.getLogger(CourierDispatchFIFO.class);

    @Override
    public int dispatchCourier(DeliveryOrder order) {
        logger.info("FIFO: a courier was sent to the kitchen to pick up the order ");
        return 0;
    }
}
