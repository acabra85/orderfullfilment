package com.acabra.orderfullfilment.couriermodule.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class SimpleResponse {
    final int statusCode;
    final String message;
    final String body;

    @JsonCreator
    protected SimpleResponse(@JsonProperty("statusCode") int statusCode, @JsonProperty("message") String message,
                             @JsonProperty("body") String body) {
        this.statusCode = statusCode;
        this.message = message;
        this.body = body;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getMessage() {
        return message;
    }

    public String getBody() {
        return body;
    }
}
