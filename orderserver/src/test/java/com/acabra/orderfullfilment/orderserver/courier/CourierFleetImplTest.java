package com.acabra.orderfullfilment.orderserver.courier;

import com.acabra.orderfullfilment.orderserver.config.CourierConfig;
import com.acabra.orderfullfilment.orderserver.courier.model.Courier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CourierFleetImplTest {

    private CourierFleet courierFleetMock = Mockito.mock()
    private List<Courier> a = Collections.emptyList();

    @Autowired
    private CourierConfig b;
    private CourierDispatchService c;

    @BeforeEach
    public void setup() {
        courierFleet = new CourierFleetImpl(a, b, c);
    }

    @Test
    void dispatch() {
    }

    @Test
    void calculateArrivalTime() {
    }

    @Test
    void release() {
    }
}