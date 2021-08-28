package com.acabra.orderfullfilment.orderserver.courier.model;
public class AssignmentDetails {

    public final int eta;

    public AssignmentDetails(int arrivalTime) {
        this.eta = arrivalTime;
    }

    public static AssignmentDetails pending(int arrivalTime) {
        return new AssignmentDetails(arrivalTime);
    }
}