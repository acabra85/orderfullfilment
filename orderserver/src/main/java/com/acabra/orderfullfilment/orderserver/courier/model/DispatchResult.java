package com.acabra.orderfullfilment.orderserver.courier.model;

import java.util.concurrent.CompletableFuture;

public class DispatchResult {
    public final CompletableFuture<Boolean> notificationFuture;
    public final Integer courierId;
    public final long ettMillis;

    /**
     * This object represents the state result of requesting a courier dispatch
     * @param courierId the id of the dispatched courier or null if no courier was dispatched
     * @param notificationFuture A future handle representing the publication status of the notification courier
     * @param ettMillis estimated travel time of courier in millis
     */
    private DispatchResult(Integer courierId, CompletableFuture<Boolean> notificationFuture, long ettMillis) {
        this.courierId = courierId;
        this.notificationFuture = notificationFuture;
        this.ettMillis = ettMillis;
    }

    public static DispatchResult notDispatched() {
        return new DispatchResult(null, null, -1);
    }

    /**
     * This method is useful when no handle notification is required
     * @param courierId id of dispatched courier, with a completion state for the notification as false
     * @param ettMillis estimated travel time of courier in millis
     * @return DispatchResult
     */
    public static DispatchResult ofCompleted(int courierId, long ettMillis) {
        return new DispatchResult(courierId, CompletableFuture.completedFuture(false), ettMillis);
    }

    /**
     *
     * Main Constructor helper
     * @param courierId courier id
     * @param notificationFuture future handle of the notification
     * @param ettMillis estimated travel time of courier in millis
     * @return a new dispatch result
     */
    public static DispatchResult of(int courierId, CompletableFuture<Boolean> notificationFuture, long ettMillis) {
       return new DispatchResult(courierId, notificationFuture, ettMillis);
    }
}