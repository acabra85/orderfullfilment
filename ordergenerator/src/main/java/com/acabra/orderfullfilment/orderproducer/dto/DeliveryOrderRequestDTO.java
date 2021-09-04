package com.acabra.orderfullfilment.orderproducer.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties
public class DeliveryOrderRequestDTO {
    public static final String SIG_PILL_ID = "SIG_PILL";
    public final long prepTime;
    public final String name;
    public final String id;
    public final boolean isSigPill;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public DeliveryOrderRequestDTO(@JsonProperty("id") String id,
                                   @JsonProperty("name") String name,
                                   @JsonProperty("prepTime") long prepTime) {
        this.prepTime = prepTime;
        this.name = name;
        this.id = id;
        this.isSigPill = SIG_PILL_ID.equals(id);
    }

    public static DeliveryOrderRequestDTO ofSigPill() {
        return new DeliveryOrderRequestDTO(SIG_PILL_ID, "", -1);
    }
}