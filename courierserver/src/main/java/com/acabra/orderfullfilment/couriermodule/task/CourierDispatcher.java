package com.acabra.orderfullfilment.couriermodule.task;

import com.acabra.orderfullfilment.couriermodule.dto.SimpleResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class CourierDispatcher {

    private static final String COURIER_ARRIVED_RESOURCE = "http://localhost:9000/orderserver/api/pickup";
    private static final HttpHeaders HEADERS = new HttpHeaders() {{setContentType(MediaType.APPLICATION_JSON);}};
    private final RestTemplate restTemplate;
    private final ScheduledExecutorService executorService;

    public CourierDispatcher(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.executorService = Executors.newScheduledThreadPool(4);
    }

    synchronized public void schedule(long timeToDestination, int courierId) {
        CompletableFuture.runAsync(() -> {
            long estimatedTimeOfArrival = System.currentTimeMillis() + 1000L * timeToDestination;
            this.executorService.schedule(
                    CourierDispatchedTask.ofCourierArrived(this, estimatedTimeOfArrival, courierId),
                    timeToDestination, TimeUnit.SECONDS);
        }).join();
    }

    public void dispatch(CourierDispatchedTask courierDispatchedTask) {
        try {
            HttpEntity<CourierDispatchedTask> request = new HttpEntity<>(courierDispatchedTask, HEADERS);
            ResponseEntity<?> response = restTemplate.postForEntity(COURIER_ARRIVED_RESOURCE, request,
                    SimpleResponse.class);
            if(HttpStatus.CREATED == response.getStatusCode()) {
                log.info("event dispatch success: " + response);
            } else {
                log.info("event dispatch failed: " + response.getBody());
            }
        } catch (Exception e) {
            log.error("Failed to notify courier arrival: {} error: {}", courierDispatchedTask, e.getMessage());
        }
    }
}
