package com.acabra.orderfullfilment.couriermodule.model;

import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

public class Courier {

    public final int id;
    public final String name;
    final static Random arrivalTime = new Random();
    private static final int TWELVE_INCLUSIVE = 13;

    private final AtomicReference<CourierStatus> status;

    private Courier(int id, String name, CourierStatus status) {
        this.id = id;
        this.name = name;
        this.status = new AtomicReference<>(status);
    }

    public static Courier ofDispatched(int id, String name) {
        return new Courier(id, name, CourierStatus.DISPATCHED);
    }

    public static Courier ofAvailable(int id, String name) {
        return new Courier(id, name, CourierStatus.AVAILABLE);
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

    public static int calculateArrivalTime() {
        return Math.abs(arrivalTime.nextInt(TWELVE_INCLUSIVE) + 3);
    }
}
