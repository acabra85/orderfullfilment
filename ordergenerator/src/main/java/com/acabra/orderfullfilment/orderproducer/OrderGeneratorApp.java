package com.acabra.orderfullfilment.orderproducer;

import com.acabra.orderfullfilment.orderproducer.dispatch.OrderDispatcher;
import com.acabra.orderfullfilment.orderproducer.dto.DeliveryOrderRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class OrderGeneratorApp {

    private static final Logger logger = LoggerFactory.getLogger(OrderGeneratorApp.class);
    public static void main(String[] args) {
        SpringApplication.run(OrderGeneratorApp.class, args);
    }

    @Bean
    public CommandLineRunner run(OrderDispatcher orderDispatcher) throws Exception {
        return args -> {
            logger.info("Starting CLI ...");
            List<DeliveryOrderRequest> orders = readOrdersFromFile();
            orderDispatcher.dispatch(orders);
            orderDispatcher.registerListener().await(orders.size(), TimeUnit.SECONDS);
            logger.info("Finishing CLI ...");
        };
    }

    public List<DeliveryOrderRequest> readOrdersFromFile() {
        try {
            URL resource = Objects.requireNonNull(OrderGeneratorApp.class.getClassLoader().getResource("small_orders.json"));
            File src = new File(resource.getFile());
            ArrayList<DeliveryOrderRequest> orders = new ObjectMapper().readValue(src, new TypeReference<>() {});
            logger.info("{} orders loaded from file!!", orders.size());
            return orders;
        } catch (IOException e) {
            logger.error(e.getMessage()+ "---" + ExceptionUtils.getRootCauseMessage(e), e);
        }
        return Collections.emptyList();
    }
}
