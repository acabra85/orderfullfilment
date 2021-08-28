package com.acabra.orderfullfilment.orderserver.courier.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.concurrent.atomic.AtomicReference;

public class Courier {

    public final Integer id;
    public final String name;

    private final AtomicReference<CourierStatus> status;

    private Courier(Integer id, String name) {
        this.id = id;
        this.name = name;
        this.status = new AtomicReference<>(CourierStatus.AVAILABLE);
    }

    private Courier(Integer id, String name, CourierStatus status) {
        this.id = id;
        this.name = name;
        this.status = new AtomicReference<>(status);
    }

    public static Courier ofDispatched(Integer id, String name) {
        return new Courier(id, name, CourierStatus.DISPATCHED);
    }

    @JsonCreator
    public static Courier ofAvailable(@JsonProperty("id") Integer id, @JsonProperty("name") String name) {
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
