package com.acabra.orderfullfilment.orderserver.kitchen.event;

public class MealReadyForPickupEvent {
    public final long mealOrderId;
    public final long readySince;

    private MealReadyForPickupEvent(long mealOrderId, long readySince) {
        this.mealOrderId = mealOrderId;
        this.readySince = readySince;
    }

    @Override
    public String toString() {
        return "MealReadyForPickupEvent{" +
                "mealOrderId=" + mealOrderId +
                ", readySince=" + readySince +
                '}';
    }

    public static MealReadyForPickupEvent of(long mealOrderId, long readySince) {
        return new MealReadyForPickupEvent(mealOrderId, readySince);
    }
}