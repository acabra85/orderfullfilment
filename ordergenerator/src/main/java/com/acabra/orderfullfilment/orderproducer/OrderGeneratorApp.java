package com.acabra.orderfullfilment.orderproducer;

import com.acabra.orderfullfilment.orderproducer.dispatch.OrderDispatcher;
import com.acabra.orderfullfilment.orderproducer.dto.DeliveryOrderRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@SpringBootApplication
@Slf4j
public class OrderGeneratorApp {

    public static void main(String[] args) {
        SpringApplication.run(OrderGeneratorApp.class, args);
    }

    @Bean
    public CommandLineRunner run(OrderDispatcher orderDispatcher) throws Exception {
        return args -> {
            log.info("Starting CLI ...");
            String param = args != null && args.length > 0 ? args[0] : null;
            List<DeliveryOrderRequest> orders = readOrdersFromFile(param);
            orderDispatcher.dispatch(orders);
            orderDispatcher.registerListener().await(orders.size(), TimeUnit.SECONDS);
            log.info("Finishing CLI ...");
        };
    }

    public List<DeliveryOrderRequest> readOrdersFromFile(String arg) {
        try {
            String resourceName = getResourceName(arg);
            URL resource = Objects.requireNonNull(OrderGeneratorApp.class.getClassLoader().getResource(resourceName));
            File src = new File(resource.getFile());
            ArrayList<DeliveryOrderRequest> orders = new ObjectMapper().readValue(src, new TypeReference<>() {});
            log.info("{} orders loaded from file!!", orders.size());
            return orders;
        } catch (IOException e) {
            log.error(e.getMessage()+ "---" + ExceptionUtils.getRootCauseMessage(e), e);
        }
        Random r = new Random();
        return new ArrayList<>(){{
            IntStream.range(0,61).forEach(id->add(new DeliveryOrderRequest(UUID.randomUUID().toString(), "n"+id, r.nextInt(6))));
        }};
    }

    private String getResourceName(String arg) {
        if(null == arg || arg.isBlank() || "TINY".equalsIgnoreCase(arg)) return "tiny-5-orders.json";
        if("SMALL".equalsIgnoreCase(arg)) return "small_orders.json";
        if("LARGE".equalsIgnoreCase(arg)) return "orders.json";
        return arg; //a filename was given
    }
}
