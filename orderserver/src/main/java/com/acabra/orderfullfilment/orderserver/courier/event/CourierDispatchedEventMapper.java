package com.acabra.orderfullfilment.orderserver.courier.event;

public class CourierDispatchedEventMapper {
    public static PickupEvent toPickupEvent(CourierDispatchedEvent event, long actualArrivalTime) {
        return new PickupEvent(event.courierId, event.eta, actualArrivalTime);
    }
}
