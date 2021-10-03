package com.acabra.orderfullfilment.orderserver.kitchen;

import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.PriorityQueue;

@Slf4j
public class KitchenLog {

    private static final KitchenLog instance = new KitchenLog();

    private final PriorityQueue<LogEntry> events = new PriorityQueue<>(Comparator.comparing(a -> a.timeStamp));

    private KitchenLog() { }

    public static KitchenLog get() {
        return instance;
    }

    synchronized public void printLog() {
        while (!events.isEmpty()) {
            log.info("{} -> {}", KitchenClock.formatted(events.peek().timeStamp), events.remove().text);
        }
    }

    public void append(long time, String message) {
        //log.info(message);
        this.events.add(LogEntry.of(time, message));
    }

    public void append(String text) {
        append(KitchenClock.now(), text);
    }

    private static class LogEntry {
        final long timeStamp;
        final String text;

        private LogEntry(long timeStamp, String text) {
            this.timeStamp = timeStamp;
            this.text = text;
        }

        static LogEntry of(long timeStamp, String message) {
            return new LogEntry(timeStamp, message);
        }
    }
}
