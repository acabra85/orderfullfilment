package com.acabra.orderfullfilment.orderproducer.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties
public class DeliveryOrderRequest {
    public static final String SIG_PILL_ID = "SIG_PILL";
    public final int prepTime;
    public final String name;
    public final String id;
    public final boolean isSigPill;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public DeliveryOrderRequest(@JsonProperty("id") String id,
                                @JsonProperty("name") String name,
                                @JsonProperty("prepTime") int prepTime) {
        this.prepTime = prepTime;
        this.name = name;
        this.id = id;
        this.isSigPill = SIG_PILL_ID.equals(id);
    }

    public static DeliveryOrderRequest ofSigPill() {
        return new DeliveryOrderRequest(SIG_PILL_ID, "", -1);
    }
}