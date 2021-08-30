package com.acabra.orderfullfilment.orderserver.dto;

import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;

public class OrderMapper {
    public DeliveryOrder fromDeliveryOrderRequest(DeliveryOrderRequest deliveryOrderRequest, long arrivalTime) {
        if(deliveryOrderRequest.prepTime < 0) {
            throw new IllegalArgumentException("Order preparation time can't be negative: " + deliveryOrderRequest.prepTime);
        }
        return new DeliveryOrder(deliveryOrderRequest.id,
                deliveryOrderRequest.name,
                deliveryOrderRequest.prepTime,
                arrivalTime);
    }
}
