package com.acabra.orderfullfilment.orderserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OrderServerConfig {

    private final int threadCount;
    private final String strategy;
    private final long periodShutDownMonitor;
    private final long pollingTimeMillis;
    private final int pollingMaxRetries;

    public OrderServerConfig(@Value("${orderserver.thread-count}") int threadCount,
                             @Value("${orderserver.strategy}") String strategy,
                             @Value("${orderserver.period-shut-down-monitor-millis}") long periodShutDownMonitor,
                             @Value("${orderserver.polling-max-retries}") int pollingMaxRetries,
                             @Value("${orderserver.polling-time-millis}") long pollingTimeMillis) {
        this.threadCount = Math.min(Math.max(threadCount, 1), Runtime.getRuntime().availableProcessors());
        this.strategy = strategy;
        this.periodShutDownMonitor = periodShutDownMonitor;
        this.pollingTimeMillis = Math.min(Math.max(0, pollingTimeMillis), 4000L);
        this.pollingMaxRetries = Math.min(Math.max(0, pollingMaxRetries), 10);
    }

    public int getThreadCount() {
        return threadCount;
    }

    public String getStrategy() {
        return strategy;
    }

    public long getPeriodShutDownMonitorSeconds() {
        return periodShutDownMonitor;
    }

    public long getPollingTimeMillis() {
        return this.pollingTimeMillis;
    }

    public int getPollingMaxRetries() {
        return pollingMaxRetries;
    }
}
