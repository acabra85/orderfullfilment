package com.acabra.orderfullfilment.orderproducer.dispatch;

import com.acabra.orderfullfilment.orderproducer.dto.DeliveryOrderRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

@Component
public class OrderDispatcher {
    private static final Logger logger = LoggerFactory.getLogger(OrderDispatcher.class);
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);
    private final RestTemplate rTemplate;
    private static final String ordersResource = "http://localhost:9000/api/orders";
    private static final HttpHeaders headers = new HttpHeaders() {{setContentType(MediaType.APPLICATION_JSON);}};
    private final LongAdder orderCounter = new LongAdder();
    private final LongAdder orderFailures = new LongAdder();

    public OrderDispatcher(RestTemplate rTemplate) {
        this.rTemplate = rTemplate;
    }

    @Bean
    RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder){
        return restTemplateBuilder.build();
    }

    public void dispatch(List<DeliveryOrderRequest> orders) {
        final Iterator<DeliveryOrderRequest> iterator = orders.iterator();
        scheduledExecutorService.scheduleAtFixedRate(() -> {
            boolean failure = false;
            for (int i = 0; i < 2 && iterator.hasNext() && !failure; i++) {
                try {
                    ResponseEntity<?> response = rTemplate.postForEntity(ordersResource, new HttpEntity<>(iterator.next(), headers), DeliveryOrderRequest.class);
                    if(HttpStatus.OK == response.getStatusCode()) {
                        orderCounter.increment();
                    } else {
                        orderFailures.increment();
                    }
                } catch (Exception e) {
                    orderFailures.increment();
                    logger.error(e.getMessage());
                    logger.error("Halting order production, no more orders ...");
                    failure = true;
                }
            }
            if(!iterator.hasNext() || failure) scheduledExecutorService.shutdown();
        }, 0L, 1L, TimeUnit.SECONDS);
    }

    public OrderDispatcherStatus totalOrders() {
        return new OrderDispatcherStatus(orderCounter.sum(), orderFailures.sum());
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
