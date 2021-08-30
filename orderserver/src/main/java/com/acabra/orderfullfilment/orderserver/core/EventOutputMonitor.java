package com.acabra.orderfullfilment.orderserver.core;

import com.acabra.orderfullfilment.orderserver.courier.CourierDispatchService;
import com.acabra.orderfullfilment.orderserver.event.OrderPreparedEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingDeque;

@Slf4j
public class EventOutputMonitor implements Runnable{
    final BlockingDeque<OutputEvent> deque;
    private final CourierDispatchService courierService;
    volatile boolean end = false;

    public EventOutputMonitor(BlockingDeque<OutputEvent> deque, CourierDispatchService courierService) {
        this.deque = deque;
        this.courierService = courierService;
    }

    @Override
    public void run() {
        Thread.currentThread().setDaemon(true);
        try {
            while(!end) {
                processOutputEvent(deque.take());
            }
        } catch (InterruptedException e) {
            log.error("Monitor interrupted, failed reading Meal Ready events");
        } catch (Exception e) {
            log.error("SFDSDSDSDS", e);
        }
    }

    private void processOutputEvent(OutputEvent outputEvent) {
        log.info(outputEvent.type.message);
        switch (outputEvent.type) {
            case ORDER_PREPARED:
                courierService.processMealReady((OrderPreparedEvent) outputEvent);
                break;
            case COURIER_ARRIVED:
                break;
            default:
                break;
        }
    }

    public void requestEnd() {
        this.end = true;
    }
}
