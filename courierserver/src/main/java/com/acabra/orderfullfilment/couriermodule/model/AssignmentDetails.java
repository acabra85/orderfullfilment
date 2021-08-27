package com.acabra.orderfullfilment.couriermodule.model;
public class AssignmentDetails {

    public final int travelTime;

    public AssignmentDetails(int arrivalTime) {
        this.travelTime = arrivalTime;
    }

    public static AssignmentDetails pending(int arrivalTime) {
        return new AssignmentDetails(arrivalTime);
    }
}