package com.acabra.orderfullfilment.orderserver.courier;

import com.acabra.orderfullfilment.orderserver.dto.CourierResponse;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

//@ConditionalOnProperty(prefix = "dispatch", name = "strategy", havingValue = "fifo")
@Component
@Slf4j
public class CourierServiceImpl implements CourierService {
    private static final String COURIER_REQUEST_DISPATCH_RESOURCE = "http://localhost:9090/courierserver/api/couriers/dispatch";
    private static final HttpHeaders HEADERS = new HttpHeaders() {{setContentType(MediaType.APPLICATION_JSON);}};
    private final RestTemplate restTemplate;

    public CourierServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public int dispatchRequest() {
        try {
            log.info("FIFO: sending a dispatch request to the courier service");
            ResponseEntity<CourierResponse> response = restTemplate.postForEntity(COURIER_REQUEST_DISPATCH_RESOURCE,
                    new HttpEntity<>(null, HEADERS),
                    CourierResponse.class);
            if(HttpStatus.CREATED == response.getStatusCode() && null != response.getBody()) {
                return response.getBody().courierId;
            }
            log.info("Failed to dispatch courier: {} status:{} ", response.getBody(), response.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to dispatch courier error: {}", e.getMessage());
        }
        return -1;
    }
}
