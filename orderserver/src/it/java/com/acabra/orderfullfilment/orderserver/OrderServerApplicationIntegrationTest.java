package com.acabra.orderfullfilment.orderserver;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource("classpath:application-integ.properties")
@Tag("integration-tests")
@Order(Order.DEFAULT)
class OrderServerApplicationIntegrationTest {
    @Test void contextLoads() {}
}
