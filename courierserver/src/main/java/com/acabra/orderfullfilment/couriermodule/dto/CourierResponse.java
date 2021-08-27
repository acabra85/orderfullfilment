package com.acabra.orderfullfilment.couriermodule.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CourierResponse {

    public final Integer courierId;
    public final String message;

    @JsonCreator
    public CourierResponse(@JsonProperty("courierId") Integer courierId, @JsonProperty("message") String message) {
        this.courierId = courierId;
        this.message = message;
    }

    public static CourierResponse ofSuccess(int courierId) {
        return new CourierResponse(courierId, "success");
    }

    public static CourierResponse ofFailure(String message) {
        return new CourierResponse(null, message);
    }

    public int getCourierId() {
        return courierId;
    }

    public String getMessage() {
        return message;
    }
}
