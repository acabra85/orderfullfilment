package com.acabra.orderfullfilment.couriermodule.task;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Event implements Runnable {
    private static final String COURIER_ARRIVED_TO_KITCHEN = "courier-arrived-to-pickup";

    public final String description;
    public final String orderId;
    public final long timeInMillis;
    public final int courierId;
    private final EventDispatcher dispatcher;

    public Event(EventDispatcher eventDispatcher, String description, String orderId, long timeInMillis, int courierId) {
        this.description = description;
        this.orderId = orderId;
        this.timeInMillis = timeInMillis;
        this.courierId = courierId;
        this.dispatcher = eventDispatcher;
    }

    public static Event ofCourierArrived(EventDispatcher eventDispatcher, long arrivalTime, int courierId) {
        return new Event(eventDispatcher, "courier-arrived-kitchen", "NO_ORDER", arrivalTime, courierId);
    }

    @Override
    public String toString() {
        return "Event{" +
                "description='" + description + '\'' +
                ", orderId='" + orderId + '\'' +
                ", timeInMillis=" + timeInMillis +
                ", courierId=" + courierId +
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
