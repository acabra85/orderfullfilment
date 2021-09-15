package com.acabra.orderfullfilment.orderserver.core.executor;

import lombok.extern.slf4j.Slf4j;

/**
 * This class is intended to be used for the executors, to prevent interruption of subsequent
 * executions when an exception is thrown
 */
@Slf4j
public abstract class SafeTask implements Runnable {
    @Override
    public void run() {
        try {
            doWork();
        } catch (Throwable t) {
            log.warn("SafeTask encountered  a throwable: {}", t.getMessage(), t);
        }
    }
    
    protected abstract void doWork();
}
