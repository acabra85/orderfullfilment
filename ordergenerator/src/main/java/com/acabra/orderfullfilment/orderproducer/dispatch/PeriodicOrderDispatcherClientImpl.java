package com.acabra.orderfullfilment.orderproducer.dispatch;

import com.acabra.orderfullfilment.orderproducer.dto.DeliveryOrderRequestDTO;
import com.acabra.orderfullfilment.orderproducer.dto.OrderDispatcherStatusPOJO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Deque;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.LongAdder;

@Service
@Slf4j
public class PeriodicOrderDispatcherClientImpl implements PeriodicOrderDispatcherClient {

    static final String ORDERS_RESOURCE = "http://localhost:9000/orderserver/api/orders";
    public static final long PERIOD = 500L;
    private final LongAdder successCount = new LongAdder();
    private final LongAdder failureCount = new LongAdder();
    private final RestTemplate restTemplate;
    private volatile boolean shutDownRequested = false;
    private final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    private final CompletableFuture<OrderDispatcherStatusPOJO> completionFuture = new CompletableFuture<>();

    public PeriodicOrderDispatcherClientImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void dispatchTwoOrdersPerSecond(final List<DeliveryOrderRequestDTO> orders) {
        Deque<DeliveryOrderRequestDTO> deque = new ConcurrentLinkedDeque<>(orders);
        CompletableFuture.runAsync(() -> {
            try {
                while(!shutDownRequested) {
                    DeliveryOrderRequestDTO take = deque.poll();
                    if(take == null || take.isSigPill) {
                        reportWorkCompleted();
                        return;
                    }
                    PostDeliveryOrderTask task = new PostDeliveryOrderTask(this::reportWorkCompleted, take,
                            successCount, failureCount, this.restTemplate);
                    CompletableFuture<Void> fut = task.getTaskCompletedFuture();
                    task.run();
                    fut.get();
                    Thread.sleep(PERIOD);
                }
            } catch (Exception e) {
                log.error("Thread interrupted: " + e.getMessage());
            } finally {
                reportWorkCompleted();
            }
        });
    }

    @Override
    public CompletableFuture<OrderDispatcherStatusPOJO> getCompletionFuture() {
        return this.completionFuture;
    }

    void reportWorkCompleted() {
        if(!shutDownRequested) {
            this.completionFuture.complete(OrderDispatcherStatusPOJO.of(this.successCount.sum(), this.failureCount.sum()));
            shutDownRequested = true;
            scheduledExecutorService.shutdown();
        }
    }
}
