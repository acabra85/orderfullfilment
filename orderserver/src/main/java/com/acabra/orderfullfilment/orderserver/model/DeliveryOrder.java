package com.acabra.orderfullfilment.orderserver.model;

import java.util.UUID;

public class DeliveryOrder {
    public final String id;
    public final String name;
    public final long prepTime;
    public final long receivedTime;

    public DeliveryOrder(String id, String name, int prepTime, long receivedTime) {
        this.id = id;
        this.name = name;
        this.prepTime = prepTime * 1000L;
        this.receivedTime = receivedTime;
    }

    public DeliveryOrder(String name, int prepTime, int receivedTime) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.prepTime = prepTime;
        this.receivedTime = receivedTime;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public long getPrepTime() {
        return prepTime;
    }

    public long getReceivedTime() {
        return receivedTime;
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
