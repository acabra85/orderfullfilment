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

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public DeliveryOrderRequest(@JsonProperty("id") String id,
                                @JsonProperty("name") String name,
                                @JsonProperty("prepTime") int prepTime) {
        this.prepTime = prepTime;
        this.name = name;
        this.id = id;
    }

    public static DeliveryOrderRequest ofSigPill() {
        return new DeliveryOrderRequest(SIG_PILL_ID, "", -1);
    }

    @Override
    public String toString() {
        return "DeliveryOrderRequest{" +
                "prepTime=" + prepTime +
                ", name='" + name + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}