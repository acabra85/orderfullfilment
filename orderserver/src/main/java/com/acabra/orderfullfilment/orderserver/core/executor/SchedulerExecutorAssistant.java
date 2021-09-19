package com.acabra.orderfullfilment.orderserver.core.executor;

import com.acabra.orderfullfilment.orderserver.config.OrderServerConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class SchedulerExecutorAssistant {
    /**
     * Helper class to manage the scheduling of tasks within the system
     */

    private final ScheduledExecutorService scheduler;
    private final ExecutorService concurrentExecutor;

    public SchedulerExecutorAssistant(OrderServerConfig config) {
        log.info("[SYSTEM] starting the scheduler with [{}] concurrent threads", config.getThreadCount());
        this.concurrentExecutor = Executors.newFixedThreadPool(config.getThreadCount());
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public boolean isOrdersMonitorTerminated() {
        return scheduler.isTerminated();
    }

    public void shutdown() {
        this.scheduler.shutdown();
        this.concurrentExecutor.shutdown();
    }

    public void scheduleAtFixedRate(SafeTask task, long delay, long period) {
        this.scheduler.scheduleAtFixedRate(() -> concurrentExecutor.submit(task),
                delay, period, TimeUnit.MILLISECONDS);
    }
}
