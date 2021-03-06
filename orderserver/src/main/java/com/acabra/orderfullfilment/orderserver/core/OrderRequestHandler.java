package com.acabra.orderfullfilment.orderserver.core;

import com.acabra.orderfullfilment.orderserver.dto.DeliveryOrderRequestDTO;
import com.acabra.orderfullfilment.orderserver.event.OrderReceivedEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEventPublisher;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Component("order_handler")
@Slf4j
public class OrderRequestHandler implements OutputEventPublisher, Consumer<DeliveryOrderRequestDTO> {

    private final AtomicReference<Queue<OutputEvent>> pubDeque = new AtomicReference<>();

    @Override
    public void accept(DeliveryOrderRequestDTO deliveryOrderRequestDTO) {
        if(deliveryOrderRequestDTO.prepTime < 0) {
            log.info("Order preparation time can't be negative: " + deliveryOrderRequestDTO.prepTime);
            return;
        }
        DeliveryOrder deliveryOrder = DeliveryOrder.of(deliveryOrderRequestDTO.id, deliveryOrderRequestDTO.name,
                deliveryOrderRequestDTO.prepTime);
        publish(OrderReceivedEvent.of(KitchenClock.now(), deliveryOrder));
    }

    @Override
    public void registerNotificationDeque(Queue<OutputEvent> deque) {
        this.pubDeque.set(deque);
    }

    @Override
    public Queue<OutputEvent> getPubDeque() {
        return pubDeque.get();
    }

    @Override
    public void logError(String msg, Throwable e) {
        log.error(msg, e);
    }
}
