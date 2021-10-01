package com.acabra.orderfullfilment.orderserver;

import com.acabra.orderfullfilment.orderserver.config.CourierConfig;
import com.acabra.orderfullfilment.orderserver.config.OrderServerConfig;
import com.acabra.orderfullfilment.orderserver.core.MetricsProcessor;
import com.acabra.orderfullfilment.orderserver.core.OrderProcessor;
import com.acabra.orderfullfilment.orderserver.core.OrderRequestHandler;
import com.acabra.orderfullfilment.orderserver.core.executor.SchedulerExecutorAssistant;
import com.acabra.orderfullfilment.orderserver.courier.CourierDispatchService;
import com.acabra.orderfullfilment.orderserver.courier.CourierFleetImpl;
import com.acabra.orderfullfilment.orderserver.courier.CourierServiceImpl;
import com.acabra.orderfullfilment.orderserver.courier.matcher.OrderCourierMatcher;
import com.acabra.orderfullfilment.orderserver.courier.matcher.OrderCourierMatcherMatchedImpl;
import com.acabra.orderfullfilment.orderserver.courier.model.Courier;
import com.acabra.orderfullfilment.orderserver.dto.DeliveryOrderRequestDTO;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenService;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenServiceImpl;
import com.acabra.orderfullfilment.orderserver.utils.EtaEstimator;
import org.assertj.core.api.Assertions;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

import static com.acabra.orderfullfilment.orderserver.UtilsIntegrationTest.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {OrderServerConfig.class, CourierConfig.class})
@TestPropertySource("classpath:application-integ.properties")
@Tag("integration-tests")
public class StrategyMatchedIntegrationTest {
    @Autowired
    private OrderServerConfig serverConfig;

    @Autowired
    private ResourceLoader resourceLoader;

    //we want to control exactly how long the travel time to the kitchen will take for every courier
    private EtaEstimator estimatorMock;

    @BeforeEach
    public void setup() {
        estimatorMock = Mockito.mock(EtaEstimator.class);
    }

    @Test
    public void mustAssignOrdersFirstComeFirstServe_givenCouriersTakeLongToArrive() throws IOException {
        // given
        ArrayList<Courier> couriers = readCouriersFromFileTestFile(resourceLoader, "classpath:it-test-couriers.json");
        List<DeliveryOrderRequestDTO> orders = readOrdersFromTestFile(resourceLoader, "classpath:it-test-orders.json");
        OrderRequestHandler orderHandler = new OrderRequestHandler();
        AtomicInteger ai = new AtomicInteger();
        Mockito.doAnswer(e -> (ai.getAndIncrement() % 3) + 9).when(estimatorMock)
                .estimateCourierTravelTimeInSeconds(Mockito.any(Courier.class));

        OrderProcessor processor = instrumentOrderSystem(couriers, estimatorMock, orderHandler);

        Iterator<DeliveryOrderRequestDTO> ordersIterator = orders.iterator();

        //when
        ScheduledExecutorService scheduledExecutorService =
                submitTheOrdersAtARateOf10PerSecond(orderHandler, ordersIterator);
        processor.getCompletedHandle().join();

        //then
        MetricsProcessor.DeliveryMetricsSnapshot actual = processor.getMetricsSnapshot();

        //verify
        Mockito.verify(estimatorMock, Mockito.times(orders.size()))
                .estimateCourierTravelTimeInSeconds(Mockito.any(Courier.class));
        Assertions.assertThat(scheduledExecutorService.isTerminated()).isTrue();
        Assertions.assertThat(actual.totalOrdersDelivered).isEqualTo(orders.size());
        Assertions.assertThat(actual.totalOrdersReceived).isEqualTo(orders.size());
        Assertions.assertThat(actual.avgFoodWaitTime).isCloseTo(6400, Offset.offset(0.0));
        Assertions.assertThat(actual.avgCourierWaitTime).isCloseTo(0, Offset.offset(0.0));
    }

    @Test
    public void mustAssignOrdersFirstComeFirstServe_givenCouriersArriveFast() throws IOException {
        // given
        ArrayList<Courier> couriers = readCouriersFromFileTestFile(resourceLoader, "classpath:it-test-couriers.json");
        List<DeliveryOrderRequestDTO> orders = readOrdersFromTestFile(resourceLoader, "classpath:it-test-orders.json");
        OrderRequestHandler orderHandler = new OrderRequestHandler();
        AtomicInteger ai = new AtomicInteger();
        Mockito.doAnswer(e -> (ai.getAndIncrement() % 2)).when(estimatorMock)
                .estimateCourierTravelTimeInSeconds(Mockito.any(Courier.class));

        OrderProcessor processor = instrumentOrderSystem(couriers, estimatorMock, orderHandler);

        Iterator<DeliveryOrderRequestDTO> ordersIterator = orders.iterator();

        //when
        ScheduledExecutorService scheduledExecutorService =
                submitTheOrdersAtARateOf10PerSecond(orderHandler, ordersIterator);
        processor.getCompletedHandle().join();

        //then
        MetricsProcessor.DeliveryMetricsSnapshot actual = processor.getMetricsSnapshot();

        //verify
        Mockito.verify(estimatorMock, Mockito.times(orders.size()))
                .estimateCourierTravelTimeInSeconds(Mockito.any(Courier.class));
        Assertions.assertThat(scheduledExecutorService.isTerminated()).isTrue();
        Assertions.assertThat(actual.totalOrdersDelivered).isEqualTo(orders.size());
        Assertions.assertThat(actual.totalOrdersReceived).isEqualTo(orders.size());
        Assertions.assertThat(actual.avgFoodWaitTime).isCloseTo(0.0d, Offset.offset(0.0));
        Assertions.assertThat(actual.avgCourierWaitTime).isCloseTo(3080.0d, Offset.offset(0.0));
    }

    private OrderProcessor instrumentOrderSystem(ArrayList<Courier> couriers, EtaEstimator estimatorMock,
                                                 OrderRequestHandler orderHandler) {
        Deque<OutputEvent> deque = new ConcurrentLinkedDeque<>();
        SchedulerExecutorAssistant scheduler = new SchedulerExecutorAssistant(serverConfig);
        CourierFleetImpl courierFleet = new CourierFleetImpl(couriers, estimatorMock);
        OrderCourierMatcher orderCourierMatcher = new OrderCourierMatcherMatchedImpl();
        CourierDispatchService courierService = new CourierServiceImpl(courierFleet, orderCourierMatcher);
        KitchenService kitchen = new KitchenServiceImpl();
        return new OrderProcessor(serverConfig, courierService, kitchen, orderHandler, deque, scheduler);
    }
}
