package com.acabra.orderfullfilment.orderserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@ConstructorBinding
@ConfigurationProperties(prefix = "orderserver")
public class OrderServerConfig {

    public static final int MINIMAL_PERIOD_TIME = 100;
    private final int threadCount;
    private final String strategy;
    private final long periodShutDownMonitorMillis;

    private final long pollingTimeMillis;
    private final int pollingMaxRetries;

    public OrderServerConfig(@Value("${orderserver.thread-count}") int threadCount,
                             @Value("${orderserver.strategy}") String strategy,
                             @Value("${orderserver.period-shut-down-monitor-millis}") long periodShutDownMonitorMillis,
                             @Value("${orderserver.polling-max-retries}") int pollingMaxRetries,
                             @Value("${orderserver.polling-time-millis}") long pollingTimeMillis) {
        this.threadCount = Math.min(Math.max(threadCount, 1), Runtime.getRuntime().availableProcessors());
        this.strategy = strategy;
        this.periodShutDownMonitorMillis = Math.max(MINIMAL_PERIOD_TIME, periodShutDownMonitorMillis);
        this.pollingTimeMillis = Math.min(Math.max(MINIMAL_PERIOD_TIME, pollingTimeMillis), 4000L);
        this.pollingMaxRetries = Math.min(Math.max(0, pollingMaxRetries), 10);
    }

    public int getThreadCount() {
        return threadCount;
    }

    public String getStrategy() {
        return strategy;
    }

    public long getPeriodShutDownMonitorMillis() {
        return periodShutDownMonitorMillis;
    }

    public long getPollingTimeMillis() {
        return this.pollingTimeMillis;
    }

    public int getPollingMaxRetries() {
        return pollingMaxRetries;
    }
}
