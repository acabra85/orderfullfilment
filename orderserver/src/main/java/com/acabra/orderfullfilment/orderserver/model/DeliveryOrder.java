package com.acabra.orderfullfilment.orderserver.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeliveryOrder {
    public final String id;
    public final String name;
    public final long prepTime;

    private DeliveryOrder(@JsonProperty("id") String id,
                          @JsonProperty("name") String name,
                          @JsonProperty("prepTime") long prepTime) {
        this.id = id;
        this.name = name;
        this.prepTime = prepTime * 1000L;
    }

    public static DeliveryOrder of(String id, String name, long prepTime) {
        return new DeliveryOrder(id, name, prepTime);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeliveryOrder that = (DeliveryOrder) o;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
