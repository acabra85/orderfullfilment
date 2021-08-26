package com.acabra.orderfullfilment.couriermodule.service;

import com.acabra.orderfullfilment.couriermodule.model.AssignmentDetails;
import com.acabra.orderfullfilment.couriermodule.model.Courier;
import com.acabra.orderfullfilment.couriermodule.task.EventDispatcher;
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
    private final EventDispatcher dispatcher;

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

    public CourierServiceImpl(EventDispatcher dispatcher) {
        this.dispatcher = dispatcher;
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
        AssignmentDetails pending = AssignmentDetails.pending(Courier.calculateArrivalTime());
        assignments.put(courier.id, pending);
        this.dispatcher.schedule(System.currentTimeMillis() + 1000L * pending.travelTime , courier.id);
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
