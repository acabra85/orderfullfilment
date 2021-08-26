package com.acabra.orderfullfilment.couriermodule.control;

import com.acabra.orderfullfilment.couriermodule.service.CourierService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/couriers")
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
    public ResponseEntity<?> dispatch() {
        int courierId = courierService.dispatch();
        return new ResponseEntity<>(courierId, HttpStatus.OK);
    }
}
