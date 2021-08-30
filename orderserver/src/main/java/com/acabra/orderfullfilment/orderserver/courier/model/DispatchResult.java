package com.acabra.orderfullfilment.orderserver.courier.model;

import java.util.concurrent.CompletableFuture;

public class DispatchResult {
    public final CompletableFuture<Boolean> notificationFuture;
    public final Integer courierId;

    /**
     * This object represents the state result of requesting a courier dispatch
     * @param courierId the id of the dispatched courier or null if no courier was dispatched
     * @param notificationFuture A future handle representing the publication status of the notification courier
     *                           ready for pickup (true,false or exception raised)
     */
    private DispatchResult(Integer courierId, CompletableFuture<Boolean> notificationFuture) {
        this.courierId = courierId;
        this.notificationFuture = notificationFuture;
    }

    public static DispatchResult notDispatched() {
        return new DispatchResult(null, null);
    }

    /**
     * This method is useful when no handle notification is required
     * @param courierId id of dispatched courier, with a completion state for the notification as false
     * @return DispatchResult
     */
    public static DispatchResult ofCompleted(int courierId) {
        return new DispatchResult(courierId, CompletableFuture.completedFuture(false));
    }

    /**
     *
     * Main Constructor helper
     * @param courierId courier id
     * @param notificationFuture future handle of the notification
     * @return a new dispatch result
     */
    public static DispatchResult of(int courierId, CompletableFuture<Boolean> notificationFuture) {
       return new DispatchResult(courierId, notificationFuture);
    }
}