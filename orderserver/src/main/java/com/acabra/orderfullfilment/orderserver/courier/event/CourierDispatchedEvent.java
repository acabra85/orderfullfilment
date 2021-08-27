package com.acabra.orderfullfilment.orderserver.courier.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CourierDispatchedEvent {
    public final long eta;
    public final int courierId;

    @JsonCreator
    public CourierDispatchedEvent(@JsonProperty("eta") long eta, @JsonProperty("courierId") int courierId) {
        this.eta = eta;
        this.courierId = courierId;
    }
}
