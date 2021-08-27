package com.acabra.orderfullfilment.orderserver.control;

import com.acabra.orderfullfilment.orderserver.courier.event.CourierDispatchedEvent;
import com.acabra.orderfullfilment.orderserver.courier.event.CourierDispatchedEventMapper;
import com.acabra.orderfullfilment.orderserver.courier.event.PickupEvent;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenClock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pickup")
@Slf4j
public class PickUpController {

    @PostMapping
    public ResponseEntity<?> pickupRequest(@RequestBody CourierDispatchedEvent event) {
        PickupEvent pickupEvent = CourierDispatchedEventMapper.toPickupEvent(event, KitchenClock.now());
        return new ResponseEntity<>(null, HttpStatus.ACCEPTED);
    }
}
