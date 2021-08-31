package com.acabra.orderfullfilment.orderserver.control;

import com.acabra.orderfullfilment.orderserver.core.OrderRequestHandler;
import com.acabra.orderfullfilment.orderserver.dto.DeliveryOrderRequest;
import com.acabra.orderfullfilment.orderserver.dto.SimpleResponse;
import com.acabra.orderfullfilment.orderserver.dto.SimpleResponseImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping(value = "/orders",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
public class OrderController {

    private final OrderRequestHandler orderRequestHandler;

    public OrderController(OrderRequestHandler handler) {
        this.orderRequestHandler = handler;
    }

    @PostMapping
    public ResponseEntity<SimpleResponse<String>> acceptOrder(@RequestBody DeliveryOrderRequest deliveryOrderRequest) {
        if (deliveryOrderRequest.prepTime >= 0) {
            CompletableFuture.runAsync(() -> this.orderRequestHandler.accept(deliveryOrderRequest));
            return ResponseEntity.of(Optional.of(new SimpleResponseImpl<>(HttpStatus.ACCEPTED.value(), "order accepted", null)));
        }
        return ResponseEntity.of(Optional.of(new SimpleResponseImpl<>(HttpStatus.BAD_REQUEST.value(), "order accepted", null)));
    }
}
