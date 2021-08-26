package com.acabra.orderfullfilment.couriermodule.service;

import com.acabra.orderfullfilment.couriermodule.model.AssignmentDetails;
import com.acabra.orderfullfilment.couriermodule.model.Courier;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CourierServiceImpl implements CourierService {

    private final HashMap<Integer, Courier> dispatchedCouriers = new HashMap<>();
    private final HashMap<Integer, AssignmentDetails> assignments = new HashMap<>();
    private final ArrayDeque<Courier> availableCouriers = new ArrayDeque<>();
    private final AtomicInteger idControl = new AtomicInteger();

    private Courier buildDispatchedCourier() {
        int id = idControl.getAndIncrement();
        return Courier.ofDispatched(id, "Courier:" + id);
    }

    private Courier findFirstAvailable() {
        if(availableCouriers.isEmpty()) throw new NoSuchElementException("No Available couriers");
        return availableCouriers.remove();
    }

    private Courier retrieveCourier() {
        Courier courier = null;
        if (availableCouriers.size() == 0) {
            courier = buildDispatchedCourier();
        } else {
            courier = findFirstAvailable();
            courier.dispatch();
        }
        return courier;
    }

    @Override
    synchronized public int matchToOrder(String orderId) {
        Courier courier = retrieveCourier();
        dispatchedCouriers.put(courier.id, courier);
        assignments.put(courier.id, AssignmentDetails.of(orderId, Courier.calculateArrivalTime()));
        return courier.id;
    }

    @Override
    synchronized public int dispatch() {
        Courier courier = retrieveCourier();
        dispatchedCouriers.put(courier.id, courier);
        assignments.put(courier.id, AssignmentDetails.pending(Courier.calculateArrivalTime()));
        return courier.id;
    }

    @Override
    synchronized public void reportOrderDelivered(int courierId) throws NoSuchElementException {
        AssignmentDetails assignmentDetails = this.assignments.get(courierId);
        if(null != assignmentDetails) {
            Courier courier = this.dispatchedCouriers.get(courierId);
            courier.orderDelivered();
            this.dispatchedCouriers.put(courierId, null);
            this.availableCouriers.add(courier);
        }
        String error = String.format("The given id [%d] does not correspond to an assigned courier", courierId);
        throw new NoSuchElementException(error);
    }
}
