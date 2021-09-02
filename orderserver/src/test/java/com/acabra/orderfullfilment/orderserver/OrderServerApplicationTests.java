package com.acabra.orderfullfilment.orderserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.concurrent.CompletableFuture;

@SpringBootTest
@TestPropertySource("classpath:application.properties")
class OrderServerApplicationTests {
    @Test void contextLoads() { }
}
