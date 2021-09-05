package com.acabra.orderfullfilment.orderserver.core;

import java.util.LongSummaryStatistics;
import java.util.concurrent.atomic.LongAdder;

public class MetricsProcessor {

    private final LongSummaryStatistics foodWaitTimeStats = new LongSummaryStatistics();
    private final LongSummaryStatistics courierWaitTimeStats = new LongSummaryStatistics();
    private final LongAdder totalOrdersReceived = new LongAdder();

    void acceptFoodWaitTime(long time) {
        foodWaitTimeStats.accept(time);
    }

    void acceptCourierWaitTime(long time) {
        courierWaitTimeStats.accept(time);
    }

    DeliveryMetricsSnapshot snapshot() {
        return DeliveryMetricsSnapshot.of(foodWaitTimeStats.getCount(),
                this.totalOrdersReceived.sum(),
                foodWaitTimeStats.getAverage(),
                courierWaitTimeStats.getAverage());
    }

    public void acceptOrderReceived() {
        this.totalOrdersReceived.increment();
    }


    public static class DeliveryMetricsSnapshot {

        public final long totalOrdersDelivered;
        public final double avgFoodWaitTime;
        public final double avgCourierWaitTime;
        public long totalOrdersReceived;

        private DeliveryMetricsSnapshot(long totalOrdersDelivered,
                                        long totalOrdersReceived,
                                        double avgFoodWaitTime,
                                        double avgCourierWaitTime) {
            this.totalOrdersDelivered = totalOrdersDelivered;
            this.totalOrdersReceived = totalOrdersReceived;
            this.avgFoodWaitTime = avgFoodWaitTime;
            this.avgCourierWaitTime = avgCourierWaitTime;
        }

        public static DeliveryMetricsSnapshot of(long totalOrdersDelivered,long totalOrdersReceived,
                                                 double avgFoodWaitTime, double avgCourierWaitTime) {
            return new DeliveryMetricsSnapshot(totalOrdersDelivered, totalOrdersReceived, avgFoodWaitTime, avgCourierWaitTime);
        }

    }
}
