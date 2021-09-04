package com.acabra.orderfullfilment.orderserver.core;

import com.acabra.orderfullfilment.orderserver.dto.DeliveryOrderRequestDTO;
import com.acabra.orderfullfilment.orderserver.event.OrderReceivedEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEventPublisher;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Component("order_handler")
@Slf4j
public class OrderRequestHandler implements OutputEventPublisher, Consumer<DeliveryOrderRequestDTO> {

    private final AtomicReference<BlockingDeque<OutputEvent>> ordersReceivedPublicationDeque = new AtomicReference<>();

    @Override
    public void accept(DeliveryOrderRequestDTO deliveryOrderRequestDTO) {
        if(deliveryOrderRequestDTO.prepTime < 0) {
            log.info("Order preparation time can't be negative: " + deliveryOrderRequestDTO.prepTime);
            return;
        }
        DeliveryOrder deliveryOrder = DeliveryOrder.of(deliveryOrderRequestDTO.id, deliveryOrderRequestDTO.name,
                deliveryOrderRequestDTO.prepTime);
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

    @Override
    public void registerNotificationDeque(BlockingDeque<OutputEvent> deque) {
        this.ordersReceivedPublicationDeque.set(deque);
    }
}
