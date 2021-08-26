package com.acabra.orderfullfilment.couriermodule.model;
public class AssignmentDetails {

    private static final String PENDING_ORDER = "PENDING_ORDER";
    public final int travelTime;
    public final String orderId;

    private AssignmentDetails(String orderId, int arrivalTime) {
        this.orderId = orderId;
        this.travelTime = arrivalTime;
    }

    public AssignmentDetails(int arrivalTime) {
        this.travelTime = arrivalTime;
        this.orderId = PENDING_ORDER;
    }

    public static AssignmentDetails of(String id, int arrivalTime) {
        return new AssignmentDetails(id, arrivalTime);
    }

    public static AssignmentDetails pending(int arrivalTime) {
        return new AssignmentDetails(arrivalTime);
    }
}