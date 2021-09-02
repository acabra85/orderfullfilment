package com.acabra.orderfullfilment.orderproducer.dispatch;

import com.acabra.orderfullfilment.orderproducer.dto.DeliveryOrderRequestDTO;
import com.acabra.orderfullfilment.orderproducer.dto.OrderDispatcherStatusPOJO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.IntStream;

@Component
@Slf4j
public class CommandLineRunnerImpl implements CommandLineRunner {

    public static final String TINY_ORDERS_JSON = "tiny-orders.json";
    public static final String SMALL_ORDERS_JSON = "small-orders.json";
    public static final String LARGE_ORDERS_JSON = "orders.json";
    private final PeriodicOrderDispatcherClient dispatcher;
    private final ResourceLoader resourceLoader;

    public CommandLineRunnerImpl(PeriodicOrderDispatcherClient dispatcher, ResourceLoader resourceLoader) {
        this.dispatcher = dispatcher;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Starting CLI ...");
        String param = args != null && args.length > 0 ? args[0] : null;
        List<DeliveryOrderRequestDTO> orders = readOrdersFromFile(param);
        dispatcher.dispatchTwoOrdersPerSecond(orders);
        OrderDispatcherStatusPOJO orderDispatchStatus = dispatcher.getCompletionFuture().get();
        log.info("Success: [{}], Failures: [{}]", orderDispatchStatus.successCount, orderDispatchStatus.failureCount);
        log.info("Finishing CLI ...");
    }

    private List<DeliveryOrderRequestDTO> readOrdersFromFile(String arg) {
        try {
            InputStream src = retrieveInputStream(arg);
            ArrayList<DeliveryOrderRequestDTO> orders = new ObjectMapper().readValue(src, new TypeReference<>() {
            });
            log.info("{} orders loaded from file!!", orders.size());
            return orders;
        } catch (IOException e) {
            log.error("Unable to read from file");
        }
        Random r = new Random();
        return new ArrayList<>() {{
            IntStream.range(0, 61)
                    .forEach(id -> add(new DeliveryOrderRequestDTO(UUID.randomUUID().toString(), "n" + id, r.nextInt(6))));
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
