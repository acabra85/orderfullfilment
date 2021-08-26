package com.acabra.orderfullfilment.couriermodule.task;

import com.acabra.orderfullfilment.couriermodule.dto.SimpleResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class EventDispatcher {

    private static final String COURIER_ARRIVED_RESOURCE = "http://localhost:9000/api/pickup";
    private static final HttpHeaders HEADERS = new HttpHeaders() {{setContentType(MediaType.APPLICATION_JSON);}};
    private final RestTemplate restTemplate;
    private final ScheduledExecutorService executorService;

    public EventDispatcher(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.executorService = Executors.newScheduledThreadPool(4);
    }

    synchronized public void schedule(long arrivalTime, int orderId) {
        this.executorService.schedule(Event.ofCourierArrived(this, arrivalTime, orderId), arrivalTime,
                TimeUnit.MILLISECONDS);
    }

    public void dispatch(Event event) {
        try {
            HttpEntity<Event> request = new HttpEntity<>(event, HEADERS);
            ResponseEntity<?> response = restTemplate.postForEntity(COURIER_ARRIVED_RESOURCE, request,
                    SimpleResponse.class);
            if(HttpStatus.CREATED == response.getStatusCode()) {
                log.info("event dispatch success: " + response);
            } else {
                log.info("event dispatch failed: " + response.getBody());
            }
        } catch (Exception e) {
            log.error("Failed to notify courier arrival: {} error: {}", event, e.getMessage());
        }
    }
}
