package com.acabra.orderfullfilment.couriermodule.service;

import com.acabra.orderfullfilment.couriermodule.model.Courier;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CourierServiceImpl implements CourierService {

    private final LinkedList<Courier> availableCouriers = new LinkedList<>();
    private final LinkedList<Courier> dispatchedCouriers = new LinkedList<>();
    private final AtomicInteger idControl = new AtomicInteger();

    @Override
    synchronized public int matchToOrder(String orderId) {
        if(availableCouriers.size() == 0) {
            Courier e = buildAssignedCourier(orderId);
            dispatchedCouriers.add(e);
            return e.id;
        }
        return matchFirstAvailable(orderId);
    }

    @Override
    public int dispatch() {
        if(availableCouriers.size() == 0) {
            Courier e = buildDispatchedCourier();
            dispatchedCouriers.add(e);
            return e.id;
        }
        return dispatchFirstAvailable();
    }

    private Courier buildDispatchedCourier() {
        int id = idControl.getAndIncrement();
        return Courier.ofDispatched(id, "Courier:" + id);
    }

    private int dispatchFirstAvailable() {
        Courier courier = availableCouriers.getFirst();
        availableCouriers.remove();
        courier.dispatch();
        dispatchedCouriers.add(courier);
        return courier.id;
    }

    private int matchFirstAvailable(String orderId) {
        Courier courier = availableCouriers.getFirst();
        availableCouriers.remove();
        courier.acceptOrder(orderId);
        dispatchedCouriers.add(courier);
        return courier.id;
    }

    private Courier buildAssignedCourier(String orderId) {
        int id = idControl.getAndIncrement();
        return Courier.ofAssigned(id, "Courier"+id, orderId);
    }
}
