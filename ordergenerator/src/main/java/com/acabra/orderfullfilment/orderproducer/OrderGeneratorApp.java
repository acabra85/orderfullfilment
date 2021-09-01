package com.acabra.orderfullfilment.orderproducer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Slf4j
public class OrderGeneratorApp {

    public static void main(String[] args) {
        SpringApplication.run(OrderGeneratorApp.class, args);
    }
}
