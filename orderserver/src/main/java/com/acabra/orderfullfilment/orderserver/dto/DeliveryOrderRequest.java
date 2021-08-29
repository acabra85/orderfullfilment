package com.acabra.orderfullfilment.orderserver.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties
public class DeliveryOrderRequest {
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
}
