package com.acabra.orderfullfilment.orderproducer.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SimpleResponseDTO {
    final String message;

    @JsonCreator
    protected SimpleResponseDTO(@JsonProperty("message") String message) {
        this.message = message;
    }
    public String getMessage() {
        return message;
    }
}
