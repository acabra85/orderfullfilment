package com.acabra.orderfullfilment.couriermodule.control;

import com.acabra.orderfullfilment.couriermodule.dto.CourierResponse;
import com.acabra.orderfullfilment.couriermodule.service.CourierService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/api/couriers",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE)
public class CourierController {

    private final CourierService courierService;

    public CourierController(CourierService courierService) {
        this.courierService = courierService;
    }

    @PostMapping
    @RequestMapping("/dispatch-matched")
    public ResponseEntity<?> matchToOder(@RequestParam("orderId") String orderId) {
        int courierId = courierService.matchToOrder(orderId);
        return new ResponseEntity<>(courierId, HttpStatus.OK);
    }

    @PostMapping
    @RequestMapping("/dispatch-fifo")
    public ResponseEntity<CourierResponse> dispatch() {
        int courierId = courierService.dispatch();
        return new ResponseEntity<>(new CourierResponse(courierId, "success"), HttpStatus.OK);
    }
}
