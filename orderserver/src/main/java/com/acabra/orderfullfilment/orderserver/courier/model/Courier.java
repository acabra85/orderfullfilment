package com.acabra.orderfullfilment.orderserver.courier.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class Courier {

    public final Integer id;
    public final String name;

    private boolean available;

    private Courier(Integer id, String name) {
        this.id = id;
        this.name = name;
        this.available = true;
    }

    private Courier(Integer id, String name, CourierStatus status) {
        this.id = id;
        this.name = name;
        this.available = CourierStatus.AVAILABLE == status;
    }

    public static Courier ofDispatched(Integer id, String name) {
        return new Courier(id, name, CourierStatus.DISPATCHED);
    }

    @JsonCreator
    public static Courier ofAvailable(@JsonProperty("id") Integer id, @JsonProperty("name") String name) {
        return new Courier(id, name);
    }

    public void dispatch() {
        if(!available) {
            throw new IllegalStateException("Courier is already dispatched");
        }
        this.available = false;
    }

    public void orderDelivered() {
        if(available) {
            throw new IllegalStateException("Courier is already available");
        }
        this.available = true;
        log.debug("Courier {} is now available", id);
    }

    public boolean isAvailable() {
        return available;
    }

    public CourierStatus getStatus() {
        return available ? CourierStatus.AVAILABLE : CourierStatus.DISPATCHED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Courier courier = (Courier) o;
        return id.equals(courier.id) && name.equals(courier.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
