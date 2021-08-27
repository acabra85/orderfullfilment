package com.acabra.orderfullfilment.orderserver.core;

import com.acabra.orderfullfilment.orderserver.courier.CourierService;
import com.acabra.orderfullfilment.orderserver.dto.DeliveryOrderRequest;
import com.acabra.orderfullfilment.orderserver.dto.OrderMapper;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class OrderProcessorImpl implements OrderProcessor {
    private static final OrderMapper orderMapper = new OrderMapper();
    private final CourierService courierService;
    private final KitchenService kitchen;

    public OrderProcessorImpl(CourierService courierService, KitchenService kitchen) {
        this.courierService = courierService;
        this.kitchen = kitchen;
    }

    @Override
    public void processOrder(DeliveryOrderRequest orderRequest) {
        DeliveryOrder order = orderMapper.fromDeliveryOrderRequest(orderRequest, KitchenClock.now());
        log.info("Order received : {} at: {}" , order.id, KitchenClock.formatted(order.receivedTime));
        final long mealId = kitchen.preRequestNextMealId(order);
        CompletableFuture
                .supplyAsync(courierService::dispatchRequest)
                .thenAccept(courierId -> {
                    kitchen.cookOrderRequest(order, mealId);
                })
                .join();
    }
}
