package com.acabra.orderfullfilment.couriermodule.model;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public class Courier {

    public final int id;
    public final String name;

    final AtomicReference<CourierStatus> status;
    final AtomicReference<String> orderId;
    transient final ReentrantLock lock = new ReentrantLock(false);

    public Courier(int id, String name, CourierStatus status, String opOrderId) {
        this.id = id;
        this.name = name;
        this.status = new AtomicReference<>(status);
        this.orderId = new AtomicReference<>(opOrderId);
    }

    public static Courier ofAssigned(int id, String name, String orderId) {
        return new Courier(id, name, CourierStatus.MATCHED, orderId);
    }

    public static Courier ofDispatched(int id, String name) {
        return new Courier(id, name, CourierStatus.DISPATCHED, null);
    }

    public void acceptOrder(String orderId) {
        this.status.set(CourierStatus.MATCHED);
        this.orderId.set(orderId);
    }

    public void dispatch() {
        this.status.set(CourierStatus.MATCHED);
    }
}
