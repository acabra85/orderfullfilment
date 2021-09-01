package com.acabra.orderfullfilment.orderproducer.dispatch;

import com.acabra.orderfullfilment.orderproducer.dto.DeliveryOrderRequest;
import com.acabra.orderfullfilment.orderproducer.dto.SimpleResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.*;

import java.util.Objects;

import static com.acabra.orderfullfilment.orderproducer.dispatch.PeriodicOrderDispatcherClientImpl.ORDERS_RESOURCE;

@Slf4j
public class PostDeliveryOrderTask implements Runnable {
    private static final HttpHeaders HEADERS = new HttpHeaders() {{setContentType(MediaType.APPLICATION_JSON);}};

    private final PeriodicOrderDispatcherClientImpl parent;
    private final DeliveryOrderRequest order;

    public PostDeliveryOrderTask(PeriodicOrderDispatcherClientImpl parent, DeliveryOrderRequest deliveryOrderRequest) {
        this.parent = parent;
        this.order = Objects.requireNonNull(deliveryOrderRequest);
    }

    @Override public void run() {
        boolean finish = false;
        try {
            if (!DeliveryOrderRequest.SIG_PILL_ID.equals(order.id)) {
                sendHttpRequest(order);
            } else {
                finish = true;
            }
        } catch (Exception e) {
            parent.incrementOrderFailures();
            log.error("Halting order production {} {}", e.getMessage(), ExceptionUtils.getRootCause(e).getMessage());
            finish = true;
        }
        if (finish) {
            parent.reportWorkCompleted();
        }
    }

    private void sendHttpRequest(DeliveryOrderRequest order) {
        HttpEntity<DeliveryOrderRequest> request = new HttpEntity<>(order, HEADERS);
        ResponseEntity<SimpleResponse> response = parent.getRestTemplate()
                    .postForEntity(ORDERS_RESOURCE, request, SimpleResponse.class);
        if (HttpStatus.Series.SUCCESSFUL == response.getStatusCode().series()) {
            parent.incrementOrderSuccesses();
        } else {
            log.info("Order was not accepted: " + Objects.requireNonNull(response.getBody()).getMessage());
            parent.incrementOrderFailures();
        }
    }

}