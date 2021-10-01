package com.acabra.orderfullfilment.orderserver;

import com.acabra.orderfullfilment.orderserver.core.OrderRequestHandler;
import com.acabra.orderfullfilment.orderserver.courier.model.Courier;
import com.acabra.orderfullfilment.orderserver.dto.DeliveryOrderRequestDTO;
import com.acabra.orderfullfilment.orderserver.utils.EtaEstimator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mockito;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class UtilsIntegrationTest {

    static ArrayList<Courier> readCouriersFromFileTestFile(ResourceLoader resourceLoader, String fileName) throws IOException {
        InputStream couriersResource = getInputStream(resourceLoader, fileName);
        return new ObjectMapper().readValue(couriersResource, new TypeReference<>(){});
    }

    static List<DeliveryOrderRequestDTO> readOrdersFromTestFile(ResourceLoader resourceLoader, String fileName) throws IOException {
        InputStream orderSource = getInputStream(resourceLoader, fileName);
        return new ObjectMapper().readValue(orderSource, new TypeReference<ArrayList<DeliveryOrderRequestDTO>>(){});
    }

    static private InputStream getInputStream(ResourceLoader resourceLoader, String resourceAsStr) throws IOException {
        return resourceLoader.getResource(resourceAsStr).getInputStream();
    }

    static ScheduledExecutorService submitTheOrdersAtARateOf10PerSecond(OrderRequestHandler orderHandler,
                                                                        Iterator<DeliveryOrderRequestDTO> ordersIterator) {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            if(!ordersIterator.hasNext()) {
                executorService.shutdown();
            } else {
                orderHandler.accept(ordersIterator.next());
            }
        },0, 100L, TimeUnit.MILLISECONDS);
        return executorService;
    }
}
