package com.acabra.orderfullfilment.orderproducer.dispatch;

import com.acabra.orderfullfilment.orderproducer.dto.DeliveryOrderRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

@Service
public class OrderDispatcher {

    static final String ORDERS_RESOURCE = "http://localhost:9000/api/orders";
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
        scheduledExecutorService.scheduleAtFixedRate(
                new PostDeliveryOrderTask(this, orders.iterator()),
                0L, //delay
                1L, TimeUnit.SECONDS);
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
