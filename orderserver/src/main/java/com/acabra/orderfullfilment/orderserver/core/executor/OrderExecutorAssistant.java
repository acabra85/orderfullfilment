package com.acabra.orderfullfilment.orderserver.core.executor;

import com.acabra.orderfullfilment.orderserver.config.OrderServerConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public class OrderExecutorAssistant {
    public static final long MONITOR_START_DELAY_MILLIS = 5000L;

    private final ExecutorService eventHandler;
    private final ExecutorService concurrentExecutor;
    private final ExecutorService noMoreOrdersMonitor;

    public OrderExecutorAssistant(OrderServerConfig config, SafeTask outputEventTask, SafeTask noMoreOrdersTask) {
        ExecutorService concurrentExecutor = Executors.newFixedThreadPool(config.getThreadCount());
        this.concurrentExecutor = concurrentExecutor;
        this.eventHandler = startOutputEventProcessors(concurrentExecutor, outputEventTask, config.getPollingTimeMillis());
        this.noMoreOrdersMonitor = startNoMoreOrdersMonitor(config.getPeriodShutDownMonitorSeconds(), noMoreOrdersTask);
    }

    private static ExecutorService startOutputEventProcessors(ExecutorService concurrentExecutor, SafeTask outputEventTask,
                                                              long periodMillis) {
        ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> concurrentExecutor.submit(outputEventTask),
                0, periodMillis, TimeUnit.MILLISECONDS);
        return scheduledExecutorService;
    }

    private static ExecutorService startNoMoreOrdersMonitor(long periodMillis, SafeTask task) {
        final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(task, MONITOR_START_DELAY_MILLIS, periodMillis, TimeUnit.MILLISECONDS);
        return scheduledExecutorService;
    }

    public void shutdown() {
        this.noMoreOrdersMonitor.shutdown();
        this.eventHandler.shutdown();
        this.concurrentExecutor.shutdown();
    }

    public boolean isOrdersMonitorTerminated() {
        return noMoreOrdersMonitor.isTerminated();
    }
}
