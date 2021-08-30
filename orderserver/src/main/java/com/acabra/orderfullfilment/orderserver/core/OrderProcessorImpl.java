package com.acabra.orderfullfilment.orderserver.core;

import com.acabra.orderfullfilment.orderserver.courier.CourierDispatchService;
import com.acabra.orderfullfilment.orderserver.dto.DeliveryOrderRequest;
import com.acabra.orderfullfilment.orderserver.dto.OrderMapper;
import com.acabra.orderfullfilment.orderserver.event.EventType;
import com.acabra.orderfullfilment.orderserver.event.OrderPreparedEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;

@Service
@Slf4j
public class OrderProcessorImpl implements OrderProcessor {
    private static final OrderMapper orderMapper = new OrderMapper();
    private final CourierDispatchService courierService;
    private final KitchenServiceImpl kitchenService;
    private final ExecutorService fixThreadPool;

    public OrderProcessorImpl(CourierDispatchService courierService, KitchenServiceImpl kitchen) {
        final BlockingDeque<OutputEvent> deque = new LinkedBlockingDeque<>();
        this.courierService = courierService;
        this.kitchenService = kitchen;
        this.kitchenService.registerMealNotificationReadyQueue(deque);
        this.fixThreadPool = Executors.newSingleThreadExecutor();
        CompletableFuture.runAsync(() -> new Thread(new OutputEventHandler(deque)).start(), fixThreadPool);
    }

    @Override
    public void processOrder(final DeliveryOrderRequest orderRequest) {
        DeliveryOrder order = orderMapper.fromDeliveryOrderRequest(orderRequest, KitchenClock.now());
        log.info("[EVENT] Order received : {} at: {}" , order.id, KitchenClock.formatted(order.receivedTime));
        final long mealReservationId = kitchenService.orderCookReservationId(order);
        CompletableFuture
                .supplyAsync(() -> courierService.dispatchRequest(order))
                .thenAccept(courierId -> {
                    if(courierId.isPresent()) {
                        kitchenService.prepareMeal(mealReservationId);
                    } else {
                        log.info("No couriers available to deliver the order ... cancelling cooking reservation");
                        kitchenService.cancelCookReservation(mealReservationId);
                    }
                });
    }

    private void processOutputEvent(final OutputEvent outputEvent) {
        log.info("Output event processed : " + outputEvent.type.message);
        if (EventType.ORDER_PREPARED == outputEvent.type) {
            courierService.processMealReady((OrderPreparedEvent) outputEvent);
        }
    }

    class OutputEventHandler implements Runnable {
        private final BlockingDeque<OutputEvent> deque;
        volatile boolean end = false;

        public OutputEventHandler(final BlockingDeque<OutputEvent> deque) {
            this.deque = deque;
        }

        @Override
        public void run() {
            try {
                while(!end) {
                    OutputEvent take = deque.take();
                    CompletableFuture.runAsync(() -> processOutputEvent(take));
                }
            } catch (InterruptedException e) {
                log.error("Monitor interrupted thread failed!!" + e.getMessage(), e);
            }
        }
    }
}
