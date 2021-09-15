package com.acabra.orderfullfilment.orderserver.control;

import com.acabra.orderfullfilment.orderserver.dto.DeliveryOrderRequestDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@RestController
@RequestMapping(value = "/orders",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
public class OrderController {

    private final Consumer<DeliveryOrderRequestDTO> orderRequestHandler;

    public OrderController(Consumer<DeliveryOrderRequestDTO> handler) {
        this.orderRequestHandler = handler;
    }

    @PostMapping
    public ResponseEntity<String> acceptOrder(@RequestBody DeliveryOrderRequestDTO deliveryOrderRequestDTO) {
        if (valid(deliveryOrderRequestDTO)) {
            CompletableFuture.runAsync(() -> this.orderRequestHandler.accept(deliveryOrderRequestDTO));
            return ResponseEntity.accepted().build();
        }
        return ResponseEntity.badRequest()
                .body("invalid request, prepTime must be >= 0: , id and name must not be null or empty"
                        + deliveryOrderRequestDTO.prepTime);
    }

    private boolean valid(DeliveryOrderRequestDTO deliveryOrderRequestDTO) {
        return deliveryOrderRequestDTO.prepTime >= 0
                && !deliveryOrderRequestDTO.id.isBlank()
                && !deliveryOrderRequestDTO.name.isBlank();
    }
}
