package com.acabra.orderfullfilment.orderserver.core.executor;

import com.acabra.orderfullfilment.orderserver.core.RetryBudget;
import com.acabra.orderfullfilment.orderserver.event.EventType;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import lombok.extern.slf4j.Slf4j;

import java.util.Deque;
import java.util.function.Supplier;

@Slf4j
public class NoMoreOrdersMonitor extends SafeTask {
    private final RetryBudget retryBudget;
    private final Supplier<Boolean> pendingDelivery;
    private final Deque<OutputEvent> deque;
    boolean run = true;

    public NoMoreOrdersMonitor(int pollingMaxRetries, Supplier<Boolean> pendingDelivery, Deque<OutputEvent> deque) {
        this.retryBudget = RetryBudget.of(pollingMaxRetries);
        this.pendingDelivery = pendingDelivery;
        this.deque = deque;
        log.info("[SYSTEM] Monitoring delivery queue");
    }

    @Override
    protected void doWork() {
        if (!run) return;
        if(retryBudget.hasMoreTokens()) {
            if(pendingDelivery.get()) {
                retryBudget.success();
            } else {
                retryBudget.spendRetryToken();
            }
        } else {
            this.deque.offer(new OutputEvent(EventType.NO_PENDING_ORDERS, KitchenClock.now()) {});
            this.run = false;
            log.info("[SYSTEM] no orders awaiting delivery");
        }
    }
}
