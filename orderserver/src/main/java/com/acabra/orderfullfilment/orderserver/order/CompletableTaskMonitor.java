package com.acabra.orderfullfilment.orderserver.order;

import com.acabra.orderfullfilment.orderserver.core.CompletableTask;
import com.acabra.orderfullfilment.orderserver.core.executor.SafeTask;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import lombok.SneakyThrows;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;

public class CompletableTaskMonitor extends SafeTask {

    private final PriorityBlockingQueue<CompletableTask> deque;

    private CompletableTaskMonitor(PriorityBlockingQueue<CompletableTask> deque) {
        this.deque = deque;
    }

    public static CompletableTaskMonitor of(PriorityBlockingQueue<CompletableTask> deque) {
        return new CompletableTaskMonitor(deque);
    }

    @SneakyThrows
    @Override
    protected void doWork() {
        long now = KitchenClock.now();
        if (!deque.isEmpty() && deque.peek().isReady(now)) {
            CompletableTask task = deque.poll(0L, TimeUnit.MILLISECONDS);
            if(task != null) {
                task.accept(now);
            }
        }
    }
}
