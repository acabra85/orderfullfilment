package com.acabra.orderfullfilment.orderserver.courier.model;

import java.util.concurrent.CompletableFuture;

public class DispatchResult {
    public final CompletableFuture<Boolean> notificationFuture;
    public final Integer courierId;
    public final int estimatedTravelTime;

    /**
     * This object represents the state result of requesting a courier dispatch
     * @param courierId the id of the dispatched courier or null if no courier was dispatched
     * @param notificationFuture A future handle representing the publication status of the notification courier
     * @param estimatedTravelTime estimated travel time of courier
     */
    private DispatchResult(Integer courierId, CompletableFuture<Boolean> notificationFuture, int estimatedTravelTime) {
        this.courierId = courierId;
        this.notificationFuture = notificationFuture;
        this.estimatedTravelTime = estimatedTravelTime;
    }

    public static DispatchResult notDispatched() {
        return new DispatchResult(null, null, -1);
    }

    /**
     * This method is useful when no handle notification is required
     * @param courierId id of dispatched courier, with a completion state for the notification as false
     * @param estimatedTravelTime estimated travel time of courier
     * @return DispatchResult
     */
    public static DispatchResult ofCompleted(int courierId, int estimatedTravelTime) {
        return new DispatchResult(courierId, CompletableFuture.completedFuture(false), estimatedTravelTime);
    }

    /**
     *
     * Main Constructor helper
     * @param courierId courier id
     * @param notificationFuture future handle of the notification
     * @param estimatedTravelTime estimated travel time of courier
     * @return a new dispatch result
     */
    public static DispatchResult of(int courierId, CompletableFuture<Boolean> notificationFuture, int estimatedTravelTime) {
       return new DispatchResult(courierId, notificationFuture, estimatedTravelTime);
    }
}