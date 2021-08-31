package com.acabra.orderfullfilment.orderproducer.dispatch;

import com.acabra.orderfullfilment.orderproducer.dto.DeliveryOrderRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class OrderDispatcher {

    static final String ORDERS_RESOURCE = "http://localhost:9000/orderserver/api/orders";
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private final LongAdder orderSuccessCounter = new LongAdder();
    private final LongAdder orderFailures = new LongAdder();
    private final LinkedList<CountDownLatch> listeners = new LinkedList<>();
    private final RestTemplate rTemplate;
    private volatile boolean shutDownRequested = false;

    public OrderDispatcher(RestTemplate restTemplate) {
        this.rTemplate = restTemplate;
    }

    public void dispatch(final List<DeliveryOrderRequest> orders) {
        Iterator<DeliveryOrderRequest> iterator = orders.iterator();
        CompletableFuture.runAsync(new PostDeliveryOrderTask(this, iterator), scheduledExecutorService);
    }


    public OrderDispatcherStatus totalOrders() {
        return new OrderDispatcherStatus(orderSuccessCounter.sum(), orderFailures.sum());
    }

    public synchronized CountDownLatch registerListener() {
        if(!this.shutDownRequested) {
            CountDownLatch lock = new CountDownLatch(1);
            listeners.add(lock);
            return lock;
        }
        return new CountDownLatch(0);
    }

    public void reportWorkCompleted() {
        shutDownRequested = true;
        listeners.iterator().forEachRemaining(CountDownLatch::countDown);
        scheduledExecutorService.shutdown();
    }

    public RestTemplate getRestTemplate() {
        return rTemplate;
    }

    public void incrementOrderSuccesses() {
        this.orderSuccessCounter.increment();
    }

    public void incrementOrderFailures() {
        this.orderFailures.increment();
    }

    public static class OrderDispatcherStatus {
        public final long success;
        public final long failures;

        public OrderDispatcherStatus(long success, long failures) {
            this.success=success;
            this.failures=failures;
        }
    }
}
