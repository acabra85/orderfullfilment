package com.acabra.orderfullfilment.orderserver.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CourierResponse {

    public final int courierId;
    public final String message;

    @JsonCreator
    public CourierResponse(@JsonProperty("courierId") int courierId, @JsonProperty("message") String message) {
        this.courierId = courierId;
        this.message = message;
    }

    public int getCourierId() {
        return courierId;
    }

    public String getMessage() {
        return message;
    }
}
