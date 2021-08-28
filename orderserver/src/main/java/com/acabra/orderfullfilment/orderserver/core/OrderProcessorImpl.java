package com.acabra.orderfullfilment.orderserver.core;

import com.acabra.orderfullfilment.orderserver.courier.CourierDispatchService;
import com.acabra.orderfullfilment.orderserver.dto.DeliveryOrderRequest;
import com.acabra.orderfullfilment.orderserver.dto.OrderMapper;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class OrderProcessorImpl implements OrderProcessor {
    private static final OrderMapper orderMapper = new OrderMapper();
    private final CourierDispatchService courierService;
    private final KitchenServiceImpl kitchenService;

    public OrderProcessorImpl(CourierDispatchService courierService, KitchenServiceImpl kitchen) {
        this.courierService = courierService;
        this.kitchenService = kitchen;
    }

    @Override
    public void processOrder(DeliveryOrderRequest orderRequest) {
        DeliveryOrder order = orderMapper.fromDeliveryOrderRequest(orderRequest, KitchenClock.now());
        log.info("[EVENT] Order received : {} at: {}" , order.id, KitchenClock.formatted(order.receivedTime));
        final long mealReservationId = kitchenService.orderCookReservationId(order);
        CompletableFuture
                .supplyAsync(() -> courierService.dispatchRequest(order))
                .thenAcceptAsync(courierId -> {
                    if(courierId.isPresent()) {
                        kitchenService.prepareMeal(mealReservationId);
                    } else {
                        log.info("No couriers available to deliver the order ...");
                        kitchenService.cancelCookReservation(mealReservationId);
                    }
                });
    }
}
