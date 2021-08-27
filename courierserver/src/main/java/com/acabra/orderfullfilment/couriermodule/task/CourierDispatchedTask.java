package com.acabra.orderfullfilment.couriermodule.task;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CourierDispatchedTask implements Runnable {
    public final long eta;
    public final int courierId;
    private final CourierDispatcher dispatcher;

    public CourierDispatchedTask(CourierDispatcher courierDispatcher, long eta, int courierId) {
        this.eta = eta;
        this.courierId = courierId;
        this.dispatcher = courierDispatcher;
    }

    public static CourierDispatchedTask ofCourierArrived(CourierDispatcher courierDispatcher, long eta, int courierId) {
        return new CourierDispatchedTask(courierDispatcher, eta, courierId);
    }

    @Override
    public String toString() {
        return "Event{" +
                "eta=" + eta +
                ", courierId=" + courierId +
                ", dispatcher=" + dispatcher +
                '}';
    }

    @Override
    public void run() {
        try {
            dispatcher.dispatch(this);
        } catch (Exception e) {
            log.error("Failed to publish event:{} error: {} ", this, e.getMessage());
        }
    }
}
