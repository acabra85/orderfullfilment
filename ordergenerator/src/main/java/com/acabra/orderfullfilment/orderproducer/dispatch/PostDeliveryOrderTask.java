package com.acabra.orderfullfilment.orderproducer.dispatch;

import com.acabra.orderfullfilment.orderproducer.dto.DeliveryOrderRequest;
import com.acabra.orderfullfilment.orderproducer.dto.SimpleResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.*;

import java.util.Iterator;

import static com.acabra.orderfullfilment.orderproducer.dispatch.OrderDispatcher.ORDERS_RESOURCE;

@Slf4j
public class PostDeliveryOrderTask implements Runnable {
    private static final HttpHeaders HEADERS = new HttpHeaders() {{setContentType(MediaType.APPLICATION_JSON);}};
    static final int TOTAL_ORDERS_PER_SECOND = 2;

    private final OrderDispatcher parent;
    private final Iterator<DeliveryOrderRequest> iterator;

    public PostDeliveryOrderTask(OrderDispatcher parent, Iterator<DeliveryOrderRequest> iterator) {
        this.parent = parent;
        this.iterator = iterator;
    }

    @Override public void run() {
        boolean finish = false;
        for (int i = 0; i < TOTAL_ORDERS_PER_SECOND && iterator.hasNext() && !finish; i++) {
            try {
                DeliveryOrderRequest order = iterator.next();
                if (!DeliveryOrderRequest.SIG_PILL_ID.equals(order.id)) {
                    HttpEntity<DeliveryOrderRequest> request = new HttpEntity<>(order, HEADERS);
                    ResponseEntity<SimpleResponse> response = parent.getRestTemplate().postForEntity(ORDERS_RESOURCE, request, SimpleResponse.class);
                    if (HttpStatus.OK == response.getStatusCode()) {
                        parent.incrementOrderSuccesses();
                    } else {
                        parent.incrementOrderFailures();
                    }
                } else {
                    finish = true;
                }
            } catch (Exception e) {
                parent.incrementOrderFailures();
                log.error(e.getMessage());
                log.error(ExceptionUtils.getRootCause(e).getMessage());
                log.error("Halting order production");
                finish = true;
            }
        }
        if (!iterator.hasNext() || finish) {
            parent.reportWorkCompleted();
        }
    }

}