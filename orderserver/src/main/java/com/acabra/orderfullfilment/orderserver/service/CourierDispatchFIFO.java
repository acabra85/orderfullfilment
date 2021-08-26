package com.acabra.orderfullfilment.orderserver.service;

import com.acabra.orderfullfilment.orderserver.dto.CourierResponse;
import com.acabra.orderfullfilment.orderserver.dto.SimpleResponse;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

@Component
@ConditionalOnProperty(prefix = "dispatch", name = "strategy", havingValue = "fifo")
@Slf4j
public class CourierDispatchFIFO implements CourierDispatchStrategy {
    private final Logger logger = LoggerFactory.getLogger(CourierDispatchFIFO.class);

    private static final String COURIER_REQUEST_DISPATCH_RESOURCE = "http://localhost:9090/api/couriers/dispatch-fifo";
    private static final HttpHeaders HEADERS = new HttpHeaders() {{setContentType(MediaType.APPLICATION_JSON);}};
    private final RestTemplate restTemplate;

    public CourierDispatchFIFO(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public int dispatchCourier(DeliveryOrder order) {
        logger.info("FIFO: a dispatch request sent to the courier service");
        try {
            HttpEntity<?> request = new HttpEntity<>(null, HEADERS);
            ResponseEntity<CourierResponse> response = restTemplate.postForEntity(COURIER_REQUEST_DISPATCH_RESOURCE, request,
                    CourierResponse.class);
            if(HttpStatus.CREATED == response.getStatusCode() && null != response.getBody()) {
                log.info("event dispatch success: " + response);
                return response.getBody().courierId;
            } else {
                log.info("event dispatch failed: " + response.getBody());
            }
        } catch (Exception e) {
            log.error("Failed to notify courier arrival: {} error: {}", order, e.getMessage());
        }
        return -1;
    }
}
