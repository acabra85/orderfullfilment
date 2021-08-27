package com.acabra.orderfullfilment.orderserver.dto;

import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;

public class OrderMapper {
    public DeliveryOrder fromDeliveryOrderRequest(DeliveryOrderRequest deliveryOrderRequest, long arrivalTime) {
        return new DeliveryOrder(deliveryOrderRequest.id,
                deliveryOrderRequest.name,
                deliveryOrderRequest.prepTime,
                arrivalTime);
    }
}
