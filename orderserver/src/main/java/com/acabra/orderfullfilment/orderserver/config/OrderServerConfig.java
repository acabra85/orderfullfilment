package com.acabra.orderfullfilment.orderserver.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OrderServerConfig {

    private final int threadCount;
    private final String strategy;

    public OrderServerConfig(@Value("${orderserver.thread-count}") int threadCount,
                             @Value("${orderserver.strategy}") String strategy) {
        this.threadCount = Math.min(Math.max(threadCount, 1), Runtime.getRuntime().availableProcessors());
        this.strategy = strategy;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public String getStrategy() {
        return strategy;
    }
}
