package com.acabra.orderfullfilment.orderserver.kitchen.event;

public class MealReadyForPickupEvent {
    public final long mealOrderId;
    public final long readySince;
    public final String deliveryOrderId;

    private MealReadyForPickupEvent(long mealOrderId, String deliveryOrderId, long readySince) {
        this.mealOrderId = mealOrderId;
        this.readySince = readySince;
        this.deliveryOrderId = deliveryOrderId;
    }

    @Override
    public String toString() {
        return "MealReadyForPickupEvent{" +
                "mealOrderId=" + mealOrderId +
                "deliveryOrderId=" + deliveryOrderId +
                ", readySince=" + readySince +
                '}';
    }

    public static MealReadyForPickupEvent of(long mealOrderId, String deliveryOrderId, long readySince) {
        return new MealReadyForPickupEvent(mealOrderId, deliveryOrderId, readySince);
    }
}