package com.acabra.orderfullfilment.orderserver.model;

import java.util.Objects;
import java.util.UUID;

public class DeliveryOrder {
    public final String id;
    public final String name;
    public final int prepTime;
    public final int receivedTime;

    public DeliveryOrder(String id, String name, int prepTime, int receivedTime) {
        this.id = id;
        this.name = name;
        this.prepTime = prepTime;
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

    public int getPrepTime() {
        return prepTime;
    }

    @Override
    public String toString() {
        return "DeliveryOrder{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", prepTime=" + prepTime +
                ", receivedTime=" + receivedTime +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeliveryOrder that = (DeliveryOrder) o;
        return id.equals(that.id) && receivedTime == that.receivedTime;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, receivedTime);
    }
}
