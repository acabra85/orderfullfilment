package com.acabra.orderfullfilment.couriermodule.service;

import com.acabra.orderfullfilment.couriermodule.task.CourierDispatcher;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {CourierDispatcher.class})
class CourierServiceImplTest {

    CourierService underTest;

    @Autowired
    CourierDispatcher courierDispatcher;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        underTest = new CourierServiceImpl(courierDispatcher);
    }
    @Test
    public void test() {
        Assertions.assertThat(underTest).isNotNull();

        int dispatch = underTest.dispatch();
    }
}