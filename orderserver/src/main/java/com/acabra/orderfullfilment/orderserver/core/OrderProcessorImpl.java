package com.acabra.orderfullfilment.orderserver.core;

import com.acabra.orderfullfilment.orderserver.dto.DeliveryOrderRequest;
import com.acabra.orderfullfilment.orderserver.dto.OrderMapper;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import com.acabra.orderfullfilment.orderserver.service.CourierDispatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OrderProcessorImpl implements OrderProcessor {

    private static final Logger logger = LoggerFactory.getLogger(OrderProcessorImpl.class);

    private final CourierDispatcher courierDispatcher;
    private static final OrderMapper orderMapper = new OrderMapper();
    private final KitchenClock clock = new KitchenClock();

    public OrderProcessorImpl(CourierDispatcher courierDispatcher) {
        this.courierDispatcher = courierDispatcher;
    }

    @Override
    public void processOrder(DeliveryOrderRequest orderRequest) {
        DeliveryOrder order = orderMapper.fromDeliveryOrderRequest(orderRequest, clock.getTime());
        logger.info("Order received : {} at: {}" , order.id, order.receivedTime);
        courierDispatcher.dispatchOrder(order);
    }

    private static final class KitchenClock {

        private static final  AtomicInteger internalClock = new AtomicInteger(0);

        public int getTime() {
            return internalClock.getAndIncrement();
        }
    }
}
