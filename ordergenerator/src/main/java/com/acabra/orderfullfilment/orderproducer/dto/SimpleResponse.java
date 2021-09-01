package com.acabra.orderfullfilment.orderproducer.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SimpleResponse {
    final String message;
    final String body;

    @JsonCreator
    protected SimpleResponse(@JsonProperty("message") String message,
                             @JsonProperty("body") String body) {
        this.message = message;
        this.body = body;
    }

    public String getMessage() {
        return message;
    }

    public String getBody() {
        return body;
    }
}
