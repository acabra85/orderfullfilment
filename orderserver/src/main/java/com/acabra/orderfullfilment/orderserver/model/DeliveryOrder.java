package com.acabra.orderfullfilment.orderserver.model;

public class DeliveryOrder {
    public final String id;
    public final String name;
    public final long prepTime;

    public DeliveryOrder(String id, String name, int prepTime) {
        this.id = id;
        this.name = name;
        this.prepTime = prepTime * 1000L;
    }

    public static DeliveryOrder of(String id, String name, int prepTime) {
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
