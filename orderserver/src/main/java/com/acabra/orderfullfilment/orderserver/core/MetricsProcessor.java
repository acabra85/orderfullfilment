package com.acabra.orderfullfilment.orderserver.core;

import java.util.LongSummaryStatistics;
import java.util.concurrent.atomic.LongAdder;

public class MetricsProcessor {

    private final LongSummaryStatistics foodWaitTimeStats = new LongSummaryStatistics();
    private final LongSummaryStatistics courierWaitTimeStats = new LongSummaryStatistics();
    private final LongAdder totalOrdersReceived = new LongAdder();
    private final LongAdder ordersPrepareRequest = new LongAdder();

    public void acceptFoodWaitTime(long time) {
        foodWaitTimeStats.accept(time);
    }

    public void acceptCourierWaitTime(long time) {
        courierWaitTimeStats.accept(time);
    }

    DeliveryMetricsSnapshot snapshot() {
        return DeliveryMetricsSnapshot.of(
                this.totalOrdersReceived.sum(),
                this.ordersPrepareRequest.sum(),
                this.foodWaitTimeStats.getCount(),
                this.foodWaitTimeStats.getAverage(),
                this.courierWaitTimeStats.getAverage());
    }

    public void acceptOrderReceived() {
        this.totalOrdersReceived.increment();
    }

    public void acceptOrderPrepareRequest() {
        this.ordersPrepareRequest.increment();
    }


    public static class DeliveryMetricsSnapshot {

        public final long totalOrdersReceived;
        public final long totalOrdersPrepared;
        public final long totalOrdersDelivered;
        public final double avgFoodWaitTime;
        public final double avgCourierWaitTime;

        private DeliveryMetricsSnapshot(
                                        long totalOrdersReceived,
                                        long totalOrdersPrepared,
                                        long totalOrdersDelivered,
                                        double avgFoodWaitTime,
                                        double avgCourierWaitTime) {
            this.totalOrdersReceived = totalOrdersReceived;
            this.totalOrdersPrepared = totalOrdersPrepared;
            this.totalOrdersDelivered = totalOrdersDelivered;
            this.avgFoodWaitTime = avgFoodWaitTime;
            this.avgCourierWaitTime = avgCourierWaitTime;
        }

        public static DeliveryMetricsSnapshot of(long totalOrdersReceived, long totalOrdersPrepared, long totalOrdersDelivered,
                                                 double avgFoodWaitTime, double avgCourierWaitTime) {
            return new DeliveryMetricsSnapshot(totalOrdersReceived, totalOrdersPrepared, totalOrdersDelivered,
                    avgFoodWaitTime, avgCourierWaitTime);
        }

    }
}
