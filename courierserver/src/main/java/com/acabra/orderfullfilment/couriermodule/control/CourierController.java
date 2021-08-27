package com.acabra.orderfullfilment.couriermodule.control;

import com.acabra.orderfullfilment.couriermodule.dto.CourierResponse;
import com.acabra.orderfullfilment.couriermodule.dto.SimpleResponse;
import com.acabra.orderfullfilment.couriermodule.service.CourierService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping(value = "/api/couriers",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
@Slf4j
public class CourierController {

    private final CourierService courierService;

    public CourierController(CourierService courierService) {
        this.courierService = courierService;
    }

    @PostMapping("/dispatch")
    public ResponseEntity<CourierResponse> dispatch() {
        try {
            int courierId = courierService.dispatch();
            log.info("Courier {} dispatched",courierId);
            return new ResponseEntity<>(CourierResponse.ofSuccess(courierId), HttpStatus.CREATED);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(CourierResponse.ofFailure(e.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/{courierId}/release")
    public ResponseEntity<SimpleResponse> release(@PathVariable int courierId) {
        try {
            courierService.release(courierId);
            log.info("Courier {} is now available", courierId);
            return new ResponseEntity<>(null, HttpStatus.OK);
        }catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        }
    }
}
