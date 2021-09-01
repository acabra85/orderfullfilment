package com.acabra.orderfullfilment.orderproducer;

import com.acabra.orderfullfilment.orderproducer.dispatch.PeriodicOrderDispatcherClientImpl;
import com.acabra.orderfullfilment.orderproducer.dto.DeliveryOrderRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ResourceLoader;

import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

@SpringBootApplication
@Slf4j
public class OrderGeneratorApp {

    public static final String TINY_ORDERS_JSON = "tiny-orders.json";
    public static final String SMALL_ORDERS_JSON = "small-orders.json";
    public static final String LARGE_ORDERS_JSON = "orders.json";
    private ResourceLoader resourceLoader;


    public static void main(String[] args) {
        SpringApplication.run(OrderGeneratorApp.class, args);
    }

    @Bean
    public CommandLineRunner run(PeriodicOrderDispatcherClientImpl orderDispatcher, ResourceLoader resourceLoader) throws Exception {
        this.resourceLoader = resourceLoader;
        return args -> {
            log.info("Starting CLI ...");
            String param = args != null && args.length > 0 ? args[0] : null;
            List<DeliveryOrderRequest> orders = readOrdersFromFile(param);
            orderDispatcher.dispatchTwoOrdersPerSecond(orders);
            orderDispatcher.registerListener().await(orders.size(), TimeUnit.SECONDS);
            log.info("Finishing CLI ...");
        };
    }

    public List<DeliveryOrderRequest> readOrdersFromFile(String arg) {
        try {
            InputStream src = retrieveInputStream(arg);
            ArrayList<DeliveryOrderRequest> orders = new ObjectMapper().readValue(src, new TypeReference<>() {
            });
            log.info("{} orders loaded from file!!", orders.size());
            return orders;
        } catch (IOException e) {
            log.error("Unable to read from file");
        }
        Random r = new Random();
        return new ArrayList<>() {{
            IntStream.range(0, 61).forEach(id -> add(new DeliveryOrderRequest(UUID.randomUUID().toString(), "n" + id, r.nextInt(6))));
        }};
    }

    private InputStream retrieveInputStream(String arg) throws IOException {
        arg = null == arg ? "" : arg;
        log.info("attempting to load orders from {}", arg);
        InputStream defaultJsonStream = resourceLoader.getResource("classpath:" + TINY_ORDERS_JSON).getInputStream();
        if(arg.isBlank()) {
            return defaultJsonStream;
        }
        List<String> validPreArgs = List.of("tiny", "large", "small");
        int idx = validPreArgs.indexOf(arg.toLowerCase(Locale.ROOT));
        switch (idx) {
            case 0:
                return defaultJsonStream;
            case 1:
                return resourceLoader.getResource("classpath:" + LARGE_ORDERS_JSON).getInputStream();
            case 2:
                return resourceLoader.getResource("classpath:" + SMALL_ORDERS_JSON).getInputStream();
            default:
                //attempt to read clients own file
                try {
                    return new FileInputStream(arg);
                } catch (Exception e) {
                    log.error("Unable to read form file <" + arg + ">");;
                    break;
                }
        }
        return defaultJsonStream;
    }
}
