package com.acabra.orderfullfilment.orderserver.kitchen;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class KitchenClock {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    public static final ZoneId UTC_PLUS_2 = ZoneId.of("Europe/Paris");

    private KitchenClock() {} //not for instantiation

    public static long now() {
        return System.currentTimeMillis();
    }

    public static String formatted(long epoch) {
        return LocalDateTime
                .ofInstant(Instant.ofEpochMilli(epoch), UTC_PLUS_2)
                .format(FORMATTER);
    }
}