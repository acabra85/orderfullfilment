package com.acabra.orderfullfilment.orderproducer.dto;

public class OrderDispatcherStatusPOJO {
    public final long successCount;
    public final long failureCount;

    private OrderDispatcherStatusPOJO(long successCount, long failureCount) {
        this.successCount = successCount;
        this.failureCount = failureCount;
    }

    public static OrderDispatcherStatusPOJO of(long successes, long failures) {
        return new OrderDispatcherStatusPOJO(successes, failures);
    }
}