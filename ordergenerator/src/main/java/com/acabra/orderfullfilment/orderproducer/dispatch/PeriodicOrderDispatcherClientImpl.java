package com.acabra.orderfullfilment.orderproducer.dispatch;

import com.acabra.orderfullfilment.orderproducer.dto.DeliveryOrderRequest;
import com.acabra.orderfullfilment.orderproducer.dto.OrderDispatcherStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.Function;

@Service
public class PeriodicOrderDispatcherClientImpl implements PeriodicOrderDispatcherClient {

    static final String ORDERS_RESOURCE = "http://localhost:9000/orderserver/api/orders";
    public static final long ONE_SECOND_TO_MILLIS = 1000;
    private final LongAdder successCount = new LongAdder();
    private final LongAdder failureCount = new LongAdder();
    private final LinkedList<CountDownLatch> listeners = new LinkedList<>();
    private final RestTemplate restTemplate;
    private volatile boolean shutDownRequested = false;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private final CompletableFuture<OrderDispatcherStatus> completionFuture = new CompletableFuture<>();

    public PeriodicOrderDispatcherClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void dispatchOrdersWithFrequency(int frequency, final List<DeliveryOrderRequest> orders) {
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
            new PostDeliveryOrderTask(this::reportWorkCompleted, take, successCount, failureCount, this.restTemplate).run();
        }, 0, ONE_SECOND_TO_MILLIS / frequency, TimeUnit.MILLISECONDS);
    }

    @Override
    public CompletableFuture<OrderDispatcherStatus> getCompletionFuture() {
        return this.completionFuture;
    }

    void reportWorkCompleted() {
        if(!shutDownRequested) {
            this.completionFuture.complete(OrderDispatcherStatus.of(this.successCount.sum(), this.failureCount.sum()));
            shutDownRequested = true;
            listeners.iterator().forEachRemaining(CountDownLatch::countDown);
            scheduledExecutorService.shutdown();
        }
    }
}
