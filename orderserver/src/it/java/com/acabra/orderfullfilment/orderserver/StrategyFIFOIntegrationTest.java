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
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ScheduledExecutorService;

import static com.acabra.orderfullfilment.orderserver.UtilsIntegrationTest.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {OrderServerConfig.class, CourierConfig.class})
@TestPropertySource("classpath:application-integ.properties")
public class StrategyFIFOIntegrationTest {

    @Autowired
    private OrderServerConfig serverConfig;

    @Autowired
    private ResourceLoader resourceLoader;

    /**
     * In this test all the orders are cooked before the drivers arrived,
     *
     */
    @Test
    public void mustAssignOrdersFirstComeFirstServe() throws IOException {
        // given
        ArrayList<Courier> couriers = readCouriersFromFileTestFile(resourceLoader, "classpath:it-test-couriers.json");
        List<DeliveryOrderRequestDTO> orders = readOrdersFromTestFile(resourceLoader, "classpath:it-test-orders.json");

        //we want to control exactly how long the travel time to the kitchen will take for every courier
        HashMap<String, Integer> travelTimesSeconds = new HashMap<>() {{
            put(couriers.get(0).name, 8); put(couriers.get(1).name, 2); put(couriers.get(2).name, 0);
        }};

        EtaEstimator estimatorMock = buildPredictableEstimatorMock(couriers, travelTimesSeconds);

        OrderRequestHandler orderHandler = new OrderRequestHandler();

        OrderProcessor processor = instrumentOrderSystem(couriers, estimatorMock, orderHandler);

        Iterator<DeliveryOrderRequestDTO> ordersIterator = orders.iterator();

        //when
        ScheduledExecutorService scheduledExecutorService =
                submitTheOrdersAtARateOf2PerSecond(orderHandler, ordersIterator);
        processor.getCompletedHandle().join();

        //then
        MetricsProcessor.DeliveryMetricsSnapshot actual = processor.getMetricsSnapshot();

        //verify
        Mockito.verify(estimatorMock, Mockito.times(orders.size())).estimateCourierTravelTimeInSeconds(Mockito.any(Courier.class));
        Assertions.assertThat(scheduledExecutorService.isTerminated()).isTrue();
        Assertions.assertThat(actual.totalOrdersDelivered).isEqualTo(orders.size());
        Assertions.assertThat(actual.totalOrdersReceived).isEqualTo(orders.size());
        Assertions.assertThat(actual.avgFoodWaitTime).isCloseTo(1500.0d, Offset.offset(2000.0));
        Assertions.assertThat(actual.avgCourierWaitTime).isCloseTo(1500.0d, Offset.offset(2000.0));
    }

    private OrderProcessor instrumentOrderSystem(ArrayList<Courier> couriers, EtaEstimator estimatorMock, OrderRequestHandler orderHandler) {
        Deque<OutputEvent> deque = new ConcurrentLinkedDeque<>();
        CourierFleetImpl courierFleet = new CourierFleetImpl(couriers, estimatorMock);
        OrderCourierMatcher orderCourierMatcher = new OrderCourierMatcherFIFOImpl();
        CourierDispatchService courierService = new CourierServiceImpl(courierFleet, orderCourierMatcher);
        KitchenService kitchen = new KitchenServiceImpl();
        return new OrderProcessor(serverConfig, courierService, kitchen, orderHandler, deque);
    }
}
