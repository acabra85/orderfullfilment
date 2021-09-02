package com.acabra.orderfullfilment.orderserver.core;

import java.util.LongSummaryStatistics;

public class MetricsProcessor {

    private final LongSummaryStatistics foodWaitTimeStats = new LongSummaryStatistics();
    private final LongSummaryStatistics courierWaitTimeStats = new LongSummaryStatistics();

    public void acceptFoodWaitTime(long time) {
        foodWaitTimeStats.accept(time);
    }

    public void acceptCourierWaitTime(long time) {
        courierWaitTimeStats.accept(time);
    }

    public double getAvgCourierWaitTime() {
        return courierWaitTimeStats.getAverage();
    }
    public double getAvgFoodWaitTime() {
        return courierWaitTimeStats.getAverage();
    }
}
