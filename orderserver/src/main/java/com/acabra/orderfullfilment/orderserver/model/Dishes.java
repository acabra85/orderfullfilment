package com.acabra.orderfullfilment.orderserver.model;

import java.util.Random;

public enum Dishes {
    CHEESE_PIZZA("Cheese Pizza", 2),
    HOT_VEGGIE_POCKET("Veggie Pocket", 3),
    BURGER("Burger", 5),
    PHILLY_CHEESE_STEAK("Philly Cheese Steak", 4);

    private static final int size = Dishes.values().length;
    private static final Random r = new Random();

    public final String description;
    public final int prepTime;

    Dishes(String description, int preparationTime) {
        this.description = description;
        this.prepTime = preparationTime;
    }

    public static Dishes getRandomDish() {
        return Dishes.values()[Math.abs(r.nextInt()) % (size - 1)];
    }
}
