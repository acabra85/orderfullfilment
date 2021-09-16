package com.acabra.orderfullfilment.orderserver.courier.matcher;

import com.acabra.orderfullfilment.orderserver.event.CourierArrivedEvent;
import com.acabra.orderfullfilment.orderserver.event.CourierDispatchedEvent;
import com.acabra.orderfullfilment.orderserver.event.OrderPreparedEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEventPublisher;

public interface OrderCourierMatcher extends OutputEventPublisher {

    /**
     * Attempts to match the order prepared to a courier if available according to the respective
     * strategy
     * @param orderPreparedEvent the event indicating a meal was prepared and ready for pickup
     * @return false if the event was not accepted or true otherwise
     * @throws NullPointerException if the orderId is not recognized as it is expected the
     *         processCourierDispatchedEvent was previously registered it
     */
    boolean acceptOrderPreparedEvent(OrderPreparedEvent orderPreparedEvent);

    /**
     * Attempts to match the courier to an order if already available according to the respective
     * strategy
     * @param courierArrivedEvent event indicating a courier arrived to the kitchen to pickup orders
     * @return false if the event was not accepted or true otherwise
     * @throws NullPointerException if the courier is not recognized, as it is expected the
     *         processCourierDispatchedEvent was previously registered it
     */
    boolean acceptCourierArrivedEvent(CourierArrivedEvent courierArrivedEvent);

    /***
     * Indicates the matcher a courier was dispatched, this may allow the matcher to take additional actions
     * such as assign the expected courier to an order etc.
     * @param courierDispatchedEvent event indicating a courier was dispatched
     */
    void processCourierDispatchedEvent(CourierDispatchedEvent courierDispatchedEvent);
}
