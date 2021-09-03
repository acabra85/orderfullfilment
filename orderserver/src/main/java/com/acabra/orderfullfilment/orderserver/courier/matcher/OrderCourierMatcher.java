package com.acabra.orderfullfilment.orderserver.courier.matcher;

import com.acabra.orderfullfilment.orderserver.event.CourierArrivedEvent;
import com.acabra.orderfullfilment.orderserver.event.OrderPreparedEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEventPublisher;

public interface OrderCourierMatcher extends OutputEventPublisher {

    boolean acceptMealPreparedEvent(OrderPreparedEvent mealReady);

    boolean acceptCourierArrivedEvent(CourierArrivedEvent courierArrivedEvent);
}
