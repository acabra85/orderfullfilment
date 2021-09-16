package com.acabra.orderfullfilment.orderserver.core;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface CompletableTask extends Consumer<Long>, Comparable<CompletableTask> {

    /**
     * Returns the expected completion time
     * @return reports the moment where this task is completed
     */
    long expectedCompletionAt();

    /**
     * The test condition defining when the task is ready
     * @param now the current time
     * @return false if now is less than the expected completed time
     */
    default boolean isReady(long now) {
        return now >= expectedCompletionAt();
    }

    @Override
    default int compareTo(CompletableTask o) {
        return Long.compare(expectedCompletionAt(), o.expectedCompletionAt());
    }

    CompletableFuture<Boolean> getCompletionFuture();
}
