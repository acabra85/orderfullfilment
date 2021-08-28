package com.acabra.orderfullfilment.orderserver.courier.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.concurrent.atomic.AtomicReference;

public class Courier {

    public final int id;
    public final String name;

    private final AtomicReference<CourierStatus> status;

    private Courier(int id, String name) {
        this.id = id;
        this.name = name;
        this.status = new AtomicReference<>(CourierStatus.AVAILABLE);
    }

    private Courier(int id, String name, CourierStatus status) {
        this.id = id;
        this.name = name;
        this.status = new AtomicReference<>(status);
    }

    public static Courier ofDispatched(int id, String name) {
        return new Courier(id, name, CourierStatus.DISPATCHED);
    }

    @JsonCreator
    public static Courier ofAvailable(@JsonProperty("id") int id, @JsonProperty("name") String name) {
        return new Courier(id, name);
    }

    public void dispatch() {
        this.status.set(CourierStatus.DISPATCHED);
    }

    public void orderDelivered() {
        if(CourierStatus.AVAILABLE == this.status.get()) {
            throw new IllegalStateException("Courier is already available");
        }
        this.status.set(CourierStatus.AVAILABLE);
    }

    public boolean isAvailable() {
        return CourierStatus.AVAILABLE == this.status.get();
    }

    public CourierStatus getStatus() {
        return this.status.get();
    }

    @Override
    public String toString() {
        return "Courier{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", status=" + status.get() +
                '}';
    }
}
