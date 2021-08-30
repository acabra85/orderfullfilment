package com.acabra.orderfullfilment.orderserver.kitchen;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class KitchenClock {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private KitchenClock() {} //not for instantiation

    public static long now() {
        return System.currentTimeMillis();
    }

    public static String formatted(long epoch) {
        return LocalDateTime
                .ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.UTC)
                .format(FORMATTER);
    }
}