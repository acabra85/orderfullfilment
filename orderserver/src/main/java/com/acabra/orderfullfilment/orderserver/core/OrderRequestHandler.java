package com.acabra.orderfullfilment.orderserver.core;

import com.acabra.orderfullfilment.orderserver.dto.DeliveryOrderRequest;
import com.acabra.orderfullfilment.orderserver.event.OrderReceivedEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
public class OrderRequestHandler {

    private final AtomicReference<BlockingDeque<OutputEvent>> ordersReceivedPublicationDeque = new AtomicReference<>();

    public void accept(DeliveryOrderRequest deliveryOrderRequest) {
        if(deliveryOrderRequest.prepTime < 0) {
            log.info("Order preparation time can't be negative: " + deliveryOrderRequest.prepTime);
            return;
        }
        DeliveryOrder deliveryOrder = DeliveryOrder.of(deliveryOrderRequest.id, deliveryOrderRequest.name,
                deliveryOrderRequest.prepTime);
        publishOrderReceivedEvent(OrderReceivedEvent.of(KitchenClock.now(), deliveryOrder));
    }

    private void publishOrderReceivedEvent(OrderReceivedEvent order) {
        if(null != this.ordersReceivedPublicationDeque.get()) {
            try {
                this.ordersReceivedPublicationDeque.get().put(order);
            } catch (InterruptedException e) {
                log.error("Unable to publish order received event");
            }
        }
    }

    public void registerNotificationDeque(BlockingDeque<OutputEvent> deque) {
        this.ordersReceivedPublicationDeque.updateAndGet(oldValue -> deque);
    }
}
