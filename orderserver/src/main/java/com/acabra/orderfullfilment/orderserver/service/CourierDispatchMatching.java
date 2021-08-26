package com.acabra.orderfullfilment.orderserver.service;

import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "dispatch", name = "strategy", havingValue = "match")
public class CourierDispatchMatching implements CourierDispatchStrategy {
    private final Logger logger = LoggerFactory.getLogger(CourierDispatchMatching.class);

    @Override
    public int dispatchCourier(DeliveryOrder order) {
        CourierFleetService courierFleetService = null;
        int courierId = courierFleetService.assignAvailableCourier(order.id);
        logger.info("Matching: Order{} matched to be picked up by courier {}", order.id, courierId);
        return 0;
    }
}
