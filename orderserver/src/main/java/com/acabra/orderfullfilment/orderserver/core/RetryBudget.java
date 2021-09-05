package com.acabra.orderfullfilment.orderserver.core;


import java.util.concurrent.atomic.LongAdder;

public class RetryBudget {
    /**
     * This Class is based on Retry Storm prevention, each thread is given a RetryBudget:
     *  1. If a retry is used (unable to do work) a retryToken is spent
     *  2. If no more retryTokens left, the thread finishes and no more retires are made
     *  3. If the call is successful the retryToken increases twice as fast as the amount of c
     */

    //
    public static final int RECOVERY_VALUE = 2;

    private final int maxRetries;
    private final LongAdder retryToken;

    private RetryBudget(int maxRetries) {
        this.maxRetries = maxRetries;
        this.retryToken = new LongAdder();
        this.retryToken.add(maxRetries);
    }

    public static RetryBudget of(int pollingMaxRetries) {
        if(pollingMaxRetries <= 0) {
            throw new IllegalArgumentException("RetryBudget needs 1 or more retries to execute give: " + pollingMaxRetries);
        }
        return new RetryBudget(pollingMaxRetries);
    }

    synchronized public void success() {
        long sum = retryToken.sum();
        if(sum < maxRetries) {
            long diff = maxRetries - sum;
            if(diff > RECOVERY_VALUE) {
                this.retryToken.add(RECOVERY_VALUE);
            }
            else {
                this.retryToken.increment();
            }
        }
    }

    public boolean hasMoreTokens() {
       return this.retryToken.sum() > 0;
    }

    public void spendRetryToken() {
        if(!hasMoreTokens()){
            throw new IllegalStateException("All retry tokens used");
        }
        this.retryToken.decrement();
    }

    public int remainingTokens() {
        return this.retryToken.intValue();
    }
}
