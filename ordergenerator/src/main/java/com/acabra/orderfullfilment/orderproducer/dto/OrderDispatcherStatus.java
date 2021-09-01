package com.acabra.orderfullfilment.orderproducer.dto;

public class OrderDispatcherStatus {
    public final long successCount;
    public final long failureCount;

    private OrderDispatcherStatus(long successCount, long failureCount) {
        this.successCount = successCount;
        this.failureCount = failureCount;
    }

    public static OrderDispatcherStatus of(long successes, long failures) {
        return new OrderDispatcherStatus(successes, failures);
    }
}