package com.acabra.orderfullfilment.orderproducer.dispatch;

import com.acabra.orderfullfilment.orderproducer.dto.DeliveryOrderRequestDTO;
import com.acabra.orderfullfilment.orderproducer.dto.SimpleResponseDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.LongAdder;

import static com.acabra.orderfullfilment.orderproducer.dispatch.PeriodicOrderDispatcherClientImpl.ORDERS_RESOURCE;

@Slf4j
public class PostDeliveryOrderTask implements Runnable {
    private static final HttpHeaders HEADERS = new HttpHeaders() {{setContentType(MediaType.APPLICATION_JSON);}};

    private final Runnable reportCompletedState;
    private final DeliveryOrderRequestDTO order;
    private final LongAdder successCount;
    private final LongAdder failureCount;
    private RestTemplate restTemplate;
    private final CompletableFuture<Void> taskCompletedFuture;

    public PostDeliveryOrderTask(Runnable reportCompletionState, DeliveryOrderRequestDTO deliveryOrderRequestDTO,
                                 LongAdder successCount, LongAdder failureCount, RestTemplate restTemplate) {
        this.reportCompletedState = reportCompletionState;
        this.order = Objects.requireNonNull(deliveryOrderRequestDTO);
        this.successCount = successCount;
        this.failureCount = failureCount;
        this.restTemplate = restTemplate;
        this.taskCompletedFuture = new CompletableFuture<>();
    }

    @Override public void run() {
        boolean finish = false;
        try {
            if (!DeliveryOrderRequestDTO.SIG_PILL_ID.equals(order.id)) {
                sendHttpRequest(order);
            } else {
                finish = true;
            }
        } catch (Exception e) {
            failureCount.increment();
            log.error("Halting order production {} {}", e.getMessage(), ExceptionUtils.getRootCause(e).getMessage());
            finish = true;
        }
        if (finish) {
            reportCompletedState.run();
        }
        this.taskCompletedFuture.complete(null);
    }

    private void sendHttpRequest(DeliveryOrderRequestDTO order) {
        HttpEntity<DeliveryOrderRequestDTO> request = new HttpEntity<>(order, HEADERS);
        ResponseEntity<SimpleResponseDTO> response = restTemplate.postForEntity(ORDERS_RESOURCE, request, SimpleResponseDTO.class);
        if (HttpStatus.Series.SUCCESSFUL == response.getStatusCode().series()) {
            successCount.increment();
        } else {
            log.info("Order was not accepted: " + Objects.requireNonNull(response.getBody()).getMessage());
            failureCount.increment();
        }
    }

    public CompletableFuture<Void> getTaskCompletedFuture() {
        return taskCompletedFuture;
    }
}