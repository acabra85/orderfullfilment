package com.acabra.orderfullfilment.orderserver.control;

import com.acabra.orderfullfilment.orderserver.core.OrderProcessor;
import com.acabra.orderfullfilment.orderserver.dto.DeliveryOrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/orders",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
public class OrderController {

    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);
    private final OrderProcessor orderProcessor;

    public OrderController(OrderProcessor orderService) {
        this.orderProcessor = orderService;
    }


    @PostMapping
    public void acceptOrder(@RequestBody DeliveryOrderRequest deliveryOrderRequest) {
        orderProcessor.processOrder(deliveryOrderRequest);
    }
}
