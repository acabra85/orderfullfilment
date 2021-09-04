package com.acabra.orderfullfilment.orderserver.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.lang.NonNull;

@JsonIgnoreProperties
public class DeliveryOrderRequestDTO {
    public final String id;
    public final String name;
    public final long prepTime;

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public DeliveryOrderRequestDTO(@JsonProperty("id") @NonNull String id,
                                   @JsonProperty("name") @NonNull String name,
                                   @JsonProperty("prepTime") long prepTime) {
        this.id = id;
        this.name = name;
        this.prepTime = prepTime;
    }
}
