package com.acabra.orderfullfilment.couriermodule.service;

import com.acabra.orderfullfilment.couriermodule.config.RestClientConfig;
import com.acabra.orderfullfilment.couriermodule.task.EventDispatcher;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {EventDispatcher.class})
class CourierServiceImplTest {

    CourierService underTest;

    @Autowired
    EventDispatcher eventDispatcher;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        underTest = new CourierServiceImpl(eventDispatcher);
    }
    @Test
    public void test() {
        Assertions.assertThat(underTest).isNotNull();

        int dispatch = underTest.dispatch();
    }
}