package com.acabra.orderfullfilment.orderserver.control;

import com.acabra.orderfullfilment.orderserver.core.OrderProcessor;
import com.acabra.orderfullfilment.orderserver.core.OrderProcessorImpl;
import com.acabra.orderfullfilment.orderserver.dto.DeliveryOrderRequest;
import com.acabra.orderfullfilment.orderserver.dto.SimpleResponse;
import com.acabra.orderfullfilment.orderserver.dto.SimpleResponseImpl;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping(value = "/orders",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
public class OrderController {

    private final OrderProcessor orderProcessor;

    public OrderController(OrderProcessor orderService) {
        this.orderProcessor = orderService;
    }


    @PostMapping
    public ResponseEntity<SimpleResponse<String>> acceptOrder(@RequestBody DeliveryOrderRequest deliveryOrderRequest) {
        orderProcessor.processOrder(deliveryOrderRequest);
        return ResponseEntity.of(Optional.of(new SimpleResponseImpl<>(HttpStatus.OK.value(), "order accepted", null)));
    }
}
