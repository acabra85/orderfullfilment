package com.acabra.orderfullfilment.orderproducer.dispatch;

import com.acabra.orderfullfilment.orderproducer.dto.DeliveryOrderRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;

@Service
public class PeriodicOrderDispatcherClientImpl implements PeriodicOrderDispatcherClient {

    static final String ORDERS_RESOURCE = "http://localhost:9000/orderserver/api/orders";
    public static final long ONE_SECOND_TO_MILLIS = 1000;
    private final LongAdder orderSuccessCounter = new LongAdder();
    private final LongAdder orderFailures = new LongAdder();
    private final LinkedList<CountDownLatch> listeners = new LinkedList<>();
    private final RestTemplate rTemplate;
    private volatile boolean shutDownRequested = false;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    public PeriodicOrderDispatcherClientImpl(RestTemplate restTemplate) {
        this.rTemplate = restTemplate;
    }

    @Override
    public void dispatchEverySecond(int frequency, final List<DeliveryOrderRequest> orders) {
        BlockingQueue<DeliveryOrderRequest> deque = new LinkedBlockingDeque<>(orders);
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            if(shutDownRequested) {
                return;
            }
            DeliveryOrderRequest take = deque.poll();
            if(take == null || take.isSigPill) {
                reportWorkCompleted();
                return;
            }
            new PostDeliveryOrderTask(this, take).run();
        }, 0, ONE_SECOND_TO_MILLIS / frequency, TimeUnit.MILLISECONDS);
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

    void reportWorkCompleted() {
        if(!shutDownRequested) {
            shutDownRequested = true;
            listeners.iterator().forEachRemaining(CountDownLatch::countDown);
            scheduledExecutorService.shutdown();
        }
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
