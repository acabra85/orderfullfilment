package com.acabra.orderfullfilment.orderserver;

import com.acabra.orderfullfilment.orderserver.config.CourierConfig;
import com.acabra.orderfullfilment.orderserver.config.OrderServerConfig;
import com.acabra.orderfullfilment.orderserver.core.MetricsProcessor;
import com.acabra.orderfullfilment.orderserver.core.OrderProcessor;
import com.acabra.orderfullfilment.orderserver.core.OrderRequestHandler;
import com.acabra.orderfullfilment.orderserver.courier.CourierDispatchService;
import com.acabra.orderfullfilment.orderserver.courier.CourierFleetImpl;
import com.acabra.orderfullfilment.orderserver.courier.CourierServiceImpl;
import com.acabra.orderfullfilment.orderserver.courier.matcher.OrderCourierMatcher;
import com.acabra.orderfullfilment.orderserver.courier.matcher.OrderCourierMatcherFIFOImpl;
import com.acabra.orderfullfilment.orderserver.courier.model.Courier;
import com.acabra.orderfullfilment.orderserver.dto.DeliveryOrderRequestDTO;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenService;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenServiceImpl;
import com.acabra.orderfullfilment.orderserver.utils.EtaEstimator;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {OrderServerConfig.class, CourierConfig.class})
@TestPropertySource("classpath:application-integ.properties")
public class StrategyFIFOIntegrationTest {

    @Autowired
    private OrderServerConfig serverConfig;

    @Autowired
    private ResourceLoader resourceLoader;

    @Test
    public void mustAssignOrdersFirstComeFirstServe() throws IOException, InterruptedException {
        // given
        int expectedOrdersDelivered = 3;
        ArrayList<Courier> couriers = readCouriersFromFileTestFile();
        List<DeliveryOrderRequestDTO> orders = readOrdersFromTestFile();

        //we want to control exactly how long the travel time to the kitchen will take for every courier
        HashMap<String, Integer> travelTimesSeconds = new HashMap<>() {{
            put(couriers.get(0).name, 5); put(couriers.get(1).name, 1); put(couriers.get(2).name, 3);
        }};

        EtaEstimator estimatorMock = buildPredictableEstimatorMock(couriers, travelTimesSeconds);

        OrderRequestHandler orderHandler = new OrderRequestHandler();

        OrderProcessor processor = instrumentOrderSystem(couriers, estimatorMock, orderHandler);

        Iterator<DeliveryOrderRequestDTO> ordersIterator = orders.iterator();

        //when
        ScheduledExecutorService scheduledExecutorService = submitTheOrdersAtARateOf2PerSecond(orderHandler, ordersIterator);
        awaitTermination(10000L);

        //then
        MetricsProcessor.DeliveryMetricsSnapshot actual = processor.getMetricsSnapshot();

        //verify
        Mockito.verify(estimatorMock, Mockito.times(3)).estimateCourierTravelTimeInSeconds(Mockito.any(Courier.class));
        Assertions.assertThat(scheduledExecutorService.isTerminated()).isTrue();
        Assertions.assertThat(actual.totalOrdersDelivered).isEqualTo(expectedOrdersDelivered);
        Assertions.assertThat(actual.totalOrdersReceived).isEqualTo(orders.size());
    }

    private void awaitTermination(long timeInMills) throws InterruptedException {
        CompletableFuture<MetricsProcessor.DeliveryMetricsSnapshot> future = CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(timeInMills);
            } catch (Exception e) {
                throw new RuntimeException();
            }
            return null;
        });
        while (!future.isDone()) {
            Thread.sleep(3000);
        }
    }

    private List<DeliveryOrderRequestDTO> readOrdersFromTestFile() throws IOException {
        InputStream orderSource = getInputStream("classpath:it-test-fifo-orders.json");
        return new ObjectMapper().readValue(orderSource, new TypeReference<ArrayList<DeliveryOrderRequestDTO>>(){});
    }

    private OrderProcessor instrumentOrderSystem(ArrayList<Courier> couriers, EtaEstimator estimatorMock, OrderRequestHandler orderHandler) {
        Deque<OutputEvent> deque = new ConcurrentLinkedDeque<>();
        CourierFleetImpl courierFleet = new CourierFleetImpl(couriers, estimatorMock);
        OrderCourierMatcher orderCourierMatcher = new OrderCourierMatcherFIFOImpl();
        CourierDispatchService courierService = new CourierServiceImpl(courierFleet, orderCourierMatcher);
        KitchenService kitchen = new KitchenServiceImpl();
        return new OrderProcessor(serverConfig, courierService, kitchen, orderHandler, deque);
    }

    private ArrayList<Courier> readCouriersFromFileTestFile() throws IOException {
        InputStream couriersResource = getInputStream("classpath:it-test-fifo-couriers.json");
        return new ObjectMapper().readValue(couriersResource, new TypeReference<>(){});
    }

    private EtaEstimator buildPredictableEstimatorMock(ArrayList<Courier> couriers, HashMap<String, Integer> travelTimesSeconds) {
        EtaEstimator estimatorMock = Mockito.mock(EtaEstimator.class);
        couriers.forEach(courier -> Mockito.when(estimatorMock.estimateCourierTravelTimeInSeconds(courier))
            .thenAnswer(invocation -> travelTimesSeconds.get(((Courier) invocation.getArgument(0)).name)));
        return estimatorMock;
    }

    private ScheduledExecutorService submitTheOrdersAtARateOf2PerSecond(OrderRequestHandler orderHandler,
                                                                        Iterator<DeliveryOrderRequestDTO> ordersIterator) {
        ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(() -> {
            if(!ordersIterator.hasNext()) {
                executorService.shutdown();
            } else {
                orderHandler.accept(ordersIterator.next());
            }
        },0, 500L, TimeUnit.MILLISECONDS);
        return executorService;
    }

    private InputStream getInputStream(String resourceAsStr) throws IOException {
        return resourceLoader.getResource(resourceAsStr).getInputStream();
    }

}
