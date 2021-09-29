package com.acabra.orderfullfilment.orderserver.core;

import com.acabra.orderfullfilment.orderserver.config.OrderServerConfig;
import com.acabra.orderfullfilment.orderserver.core.executor.SchedulerExecutorAssistant;
import com.acabra.orderfullfilment.orderserver.courier.CourierDispatchService;
import com.acabra.orderfullfilment.orderserver.courier.CourierServiceImpl;
import com.acabra.orderfullfilment.orderserver.event.*;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenService;
import com.acabra.orderfullfilment.orderserver.kitchen.KitchenServiceImpl;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;


@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {OrderServerConfig.class})
@TestPropertySource(value = "classpath:application.properties")
class OrderProcessorTest {

    public static final DeliveryOrder bananaSplitOrder = DeliveryOrder.of("ORDER-ID", "BANANA-split", 5);
    private OrderProcessor underTest;

    private final OrderServerConfig config;
    private OutputEventPublisher outputEventPublisherMock;
    private CourierDispatchService courierServiceMock;
    private KitchenService kitchenServiceMock;
    private SchedulerExecutorAssistant scheduler;

    OrderProcessorTest(@Autowired OrderServerConfig config) {
        this.config = config;
    }

    @BeforeEach
    public void setup() {
        underTest = null;
        courierServiceMock = Mockito.mock(CourierServiceImpl.class);
        kitchenServiceMock = Mockito.mock(KitchenServiceImpl.class);
        outputEventPublisherMock = Mockito.mock(OrderRequestHandler.class);
        this.scheduler = new SchedulerExecutorAssistant(this.config);
    }

    @AfterEach
    public void tearDown() {
        Mockito.verifyNoMoreInteractions(kitchenServiceMock, courierServiceMock, outputEventPublisherMock);
        outputEventPublisherMock = null;
        kitchenServiceMock = null;
        courierServiceMock = null;
    }

    @Test
    void mustProcessOrderPreparedEvent() throws InterruptedException {
        //given
        Deque<OutputEvent> deque = new ConcurrentLinkedDeque<>();
        OrderPreparedEvent orderPreparedEventStub = Mockito.mock(OrderPreparedEvent.class);

        //#setup
        Mockito.doReturn(EventType.ORDER_PREPARED).when(orderPreparedEventStub).getType();

        //#setup courier
        Mockito.doNothing()
                .when(courierServiceMock).shutdown();
        Mockito.doNothing().when(courierServiceMock).registerNotificationDeque(deque);
        Mockito.when(courierServiceMock.processOrderPrepared(orderPreparedEventStub))
                .thenReturn(true);

        //#setup kitchen
        Mockito.doNothing()
                .when(kitchenServiceMock).shutdown();
        Mockito.doNothing().when(kitchenServiceMock).registerNotificationDeque(deque);

        //#setup handler
        Mockito.doNothing().when(outputEventPublisherMock).registerNotificationDeque(deque);
        underTest = new OrderProcessor(config, courierServiceMock, kitchenServiceMock, outputEventPublisherMock,
                deque, this.scheduler);

        //when
        deque.offer(orderPreparedEventStub); //send the event on the queue for processing
        Thread.sleep(2000L);
        MetricsProcessor.DeliveryMetricsSnapshot metricsSnapshot = underTest.getMetricsSnapshot();
        underTest.close();
        underTest.getCompletedHandle().join();


        //then
        Assertions.assertThat(metricsSnapshot.totalOrdersReceived).isEqualTo(0);
        Assertions.assertThat(metricsSnapshot.totalOrdersPrepared).isEqualTo(0);
        Assertions.assertThat(metricsSnapshot.totalOrdersDelivered).isEqualTo(0);

        //verify mocks
        Mockito.verify(courierServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(courierServiceMock, Mockito.times(1)).processOrderPrepared(orderPreparedEventStub);
        Mockito.verify(courierServiceMock, Mockito.times(1)).shutdown();
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).shutdown();
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(outputEventPublisherMock, Mockito.times(1)).registerNotificationDeque(deque);
    }

    @Test
    void mustProcessCourierArrivedEvent() throws InterruptedException {
        //given
        Deque<OutputEvent> deque = new ConcurrentLinkedDeque<>();
        CourierArrivedEvent courierArrivedEvent =  Mockito.mock(CourierArrivedEvent.class);

        //#setup
        Mockito.doReturn(EventType.COURIER_ARRIVED).when(courierArrivedEvent).getType();
        //#setup courier
        Mockito.doNothing()
                .when(courierServiceMock).shutdown();
        Mockito.doNothing().when(courierServiceMock).registerNotificationDeque(deque);
        Mockito.doReturn(true).when(courierServiceMock).processCourierArrived(courierArrivedEvent);
        //#setup kitchen
        Mockito.doNothing().when(kitchenServiceMock).registerNotificationDeque(deque);
        //#setup handler
        Mockito.doNothing().when(outputEventPublisherMock).registerNotificationDeque(deque);

        underTest = new OrderProcessor(config, courierServiceMock, kitchenServiceMock, outputEventPublisherMock,
                deque, this.scheduler);

        //when
        deque.offer(courierArrivedEvent); //send the event on the queue for processing
        Thread.sleep(2000L); //we force close of resources since unit test is only for one event.
        MetricsProcessor.DeliveryMetricsSnapshot metricsSnapshot = underTest.getMetricsSnapshot();
        underTest.close();
        underTest.getCompletedHandle().join();

        //then
        Assertions.assertThat(metricsSnapshot.totalOrdersReceived).isEqualTo(0);
        Assertions.assertThat(metricsSnapshot.totalOrdersPrepared).isEqualTo(0);
        Assertions.assertThat(metricsSnapshot.totalOrdersDelivered).isEqualTo(0);

        //verify mocks
        Mockito.verify(courierServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(courierServiceMock, Mockito.times(1)).processCourierArrived(Mockito.any(CourierArrivedEvent.class));
        Mockito.verify(courierServiceMock, Mockito.times(1)).shutdown();
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).shutdown();
        Mockito.verify(outputEventPublisherMock, Mockito.times(1)).registerNotificationDeque(deque);
    }

    @Test
    void mustProcessCourierDispatchedEvent() throws InterruptedException {
        //given
        Deque<OutputEvent> deque = new ConcurrentLinkedDeque<>();
        CourierDispatchedEvent courierDispatchedEvent = Mockito.mock(CourierDispatchedEvent.class);

        //#setup
        Mockito.doReturn(EventType.COURIER_DISPATCHED).when(courierDispatchedEvent).getType();

        //#setup courier
        Mockito.doNothing().when(courierServiceMock).shutdown();
        Mockito.doNothing().when(courierServiceMock).registerNotificationDeque(deque);
        Mockito.doNothing().when(courierServiceMock).processCourierDispatchedEvent(courierDispatchedEvent);
        //#setup kitchen
        Mockito.doNothing().when(kitchenServiceMock).registerNotificationDeque(deque);
        //#setup handler
        Mockito.doNothing().when(outputEventPublisherMock).registerNotificationDeque(deque);

        underTest = new OrderProcessor(config, courierServiceMock, kitchenServiceMock, outputEventPublisherMock,
                deque, this.scheduler);

        //when
        deque.offer(courierDispatchedEvent); //send the event on the queue for processing
        Thread.sleep(2000L); //we force close of resources since unit test is only for one event.
        MetricsProcessor.DeliveryMetricsSnapshot metricsSnapshot = underTest.getMetricsSnapshot();
        underTest.close();
        underTest.getCompletedHandle().join();

        //then
        Assertions.assertThat(metricsSnapshot.totalOrdersReceived).isEqualTo(0);
        Assertions.assertThat(metricsSnapshot.totalOrdersPrepared).isEqualTo(0);
        Assertions.assertThat(metricsSnapshot.totalOrdersDelivered).isEqualTo(0);

        //verify mocks
        Mockito.verify(courierServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(courierServiceMock, Mockito.times(1))
                .processCourierDispatchedEvent(Mockito.any(CourierDispatchedEvent.class));
        Mockito.verify(courierServiceMock, Mockito.times(1)).shutdown();
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).shutdown();
        Mockito.verify(outputEventPublisherMock, Mockito.times(1)).registerNotificationDeque(deque);
    }

    @Test
    void mustProcessOrderReceivedEvent_givenThatCourierDispatchedAndKitchenAcceptsReservation() throws InterruptedException {
        //given
        Integer courierId = 1;
        long cookReservationId = 0L;
        Deque<OutputEvent> deque = new ConcurrentLinkedDeque<>();
        OrderReceivedEvent orderReceivedEvent = OrderReceivedEvent.of(5, bananaSplitOrder);

        //#setup courier
        Mockito.doNothing()
                .when(courierServiceMock).shutdown();
        Mockito.doNothing()
                .when(courierServiceMock).registerNotificationDeque(deque);
        Mockito.doReturn(Optional.of(courierId))
                .when(courierServiceMock).dispatchRequest(bananaSplitOrder, cookReservationId);

        //#setup kitchen
        Mockito.doNothing()
                .when(kitchenServiceMock).shutdown();
        Mockito.doNothing()
                .when(kitchenServiceMock).registerNotificationDeque(deque);
        Mockito.doReturn(cookReservationId)
                .when(kitchenServiceMock).provideReservationId(bananaSplitOrder);
        Mockito.doReturn(CompletableFuture.completedFuture(true))
                .when(kitchenServiceMock).prepareMeal(cookReservationId);

        //#setup handler
        Mockito.doNothing().when(outputEventPublisherMock).registerNotificationDeque(deque);
        underTest = new OrderProcessor(config, courierServiceMock, kitchenServiceMock, outputEventPublisherMock,
                deque, this.scheduler);

        //when
        deque.offer(orderReceivedEvent);
        /*We must stop this call as the natural termination depends on orders being delivered
        * since this unit test only wants to test the event orderReceivedEvent it is reasonable
        * to stop it.
        * */
        Thread.sleep(2000L);
        MetricsProcessor.DeliveryMetricsSnapshot metricsSnapshot = underTest.getMetricsSnapshot();
        underTest.close();
        underTest.getCompletedHandle().join();


        //then
        Assertions.assertThat(metricsSnapshot.totalOrdersReceived).isEqualTo(1);
        Assertions.assertThat(metricsSnapshot.totalOrdersPrepared).isEqualTo(1);
        Assertions.assertThat(metricsSnapshot.totalOrdersDelivered).isEqualTo(0);

        //verify mocks
        Mockito.verify(courierServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(courierServiceMock, Mockito.times(1)).dispatchRequest(bananaSplitOrder, cookReservationId);
        Mockito.verify(courierServiceMock, Mockito.times(1)).shutdown();
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).shutdown();
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).provideReservationId(bananaSplitOrder);
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).prepareMeal(cookReservationId);
        Mockito.verify(outputEventPublisherMock, Mockito.times(1)).registerNotificationDeque(deque);
    }

    @Test
    void mustProcessOrderReceivedEvent_givenThatNoCouriersAvailableAndKitchenAcceptsReservation() throws InterruptedException {
        //given
        long cookReservationId = 0L;
        Deque<OutputEvent> deque = new ConcurrentLinkedDeque<>();
        OrderReceivedEvent orderReceivedEvent = OrderReceivedEvent.of(5, bananaSplitOrder);

        //#setup courier
        Mockito.doNothing()
                .when(courierServiceMock).registerNotificationDeque(deque);
        Mockito.doReturn(Optional.empty())
                .when(courierServiceMock).dispatchRequest(bananaSplitOrder, cookReservationId);
        Mockito.doNothing()
                .when(courierServiceMock).shutdown();

        //#setup kitchen
        Mockito.doNothing()
                .when(kitchenServiceMock).registerNotificationDeque(deque);
        Mockito.doReturn(cookReservationId)
                .when(kitchenServiceMock).provideReservationId(bananaSplitOrder);
        Mockito.doReturn(true)
                .when(kitchenServiceMock).cancelCookReservation(cookReservationId);
        Mockito.doNothing()
                .when(kitchenServiceMock).shutdown();

        //#setup handler
        Mockito.doNothing().when(outputEventPublisherMock).registerNotificationDeque(deque);
        underTest = new OrderProcessor(config, courierServiceMock, kitchenServiceMock, outputEventPublisherMock,
                deque, this.scheduler);

        //when
        deque.offer(orderReceivedEvent);
        Thread.sleep(2000L);
        MetricsProcessor.DeliveryMetricsSnapshot metricsSnapshot = underTest.getMetricsSnapshot();
        underTest.close();
        underTest.getCompletedHandle().join();

        //then
        Assertions.assertThat(metricsSnapshot.totalOrdersReceived).isEqualTo(1);
        Assertions.assertThat(metricsSnapshot.totalOrdersPrepared).isEqualTo(0);
        Assertions.assertThat(metricsSnapshot.totalOrdersDelivered).isEqualTo(0);

        //verify mocks
        Mockito.verify(courierServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(courierServiceMock, Mockito.times(1)).dispatchRequest(bananaSplitOrder, cookReservationId);
        Mockito.verify(courierServiceMock, Mockito.times(1)).shutdown();
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).shutdown();
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).provideReservationId(bananaSplitOrder);
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).cancelCookReservation(cookReservationId);
        Mockito.verify(outputEventPublisherMock, Mockito.times(1)).registerNotificationDeque(deque);
    }

    @Test
    void mustProcessOrderPickedUpEvent_andDeliveryEvent_instantDelivery() throws InterruptedException {
        //given
        int courierId = 1;
        long kitchenReservationId = 0L;
        Deque<OutputEvent> deque = new ConcurrentLinkedDeque<>();
        OrderPickedUpEvent orderPickedUpEventStub = OrderPickedUpEvent
                .of(10000L, CourierArrivedEvent.of(courierId, 250, 9800L),
                        OrderPreparedEvent.of(kitchenReservationId, "order-id", 900L), true, 500L);
        OrderDeliveredEvent orderDeliveredEvent = OrderDeliveredEvent.of(orderPickedUpEventStub);

        //#setup courier
        Mockito.doNothing().when(courierServiceMock).registerNotificationDeque(deque);
        Mockito.doNothing().when(courierServiceMock).shutdown();
        Mockito.doNothing().when(courierServiceMock).processOrderDelivered(orderDeliveredEvent);

        //#setup kitchen
        Mockito.doNothing().when(kitchenServiceMock).registerNotificationDeque(deque);
        Mockito.doNothing().when(kitchenServiceMock).shutdown();

        //#setup handler
        Mockito.doNothing().when(outputEventPublisherMock).registerNotificationDeque(deque);
        underTest = new OrderProcessor(config, courierServiceMock, kitchenServiceMock, outputEventPublisherMock,
                deque, this.scheduler);

        //when
        deque.offer(orderPickedUpEventStub);
        Thread.sleep(2000L);
        MetricsProcessor.DeliveryMetricsSnapshot metricsSnapshot = underTest.getMetricsSnapshot();
        underTest.close();
        underTest.getCompletedHandle().join();

        //then
        Assertions.assertThat(metricsSnapshot.totalOrdersReceived).isEqualTo(0);
        Assertions.assertThat(metricsSnapshot.totalOrdersPrepared).isEqualTo(0);
        Assertions.assertThat(metricsSnapshot.totalOrdersDelivered).isEqualTo(1);

        //verify mocks
        Mockito.verify(courierServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(courierServiceMock, Mockito.times(1)).processOrderDelivered(Mockito.any(OrderDeliveredEvent.class));
        Mockito.verify(courierServiceMock, Mockito.times(1)).shutdown();
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).shutdown();
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(outputEventPublisherMock, Mockito.times(1)).registerNotificationDeque(deque);
    }

    @Test
    void mustFinishNoOrdersReceived() {
        //given
        int courierId = 1;
        long kitchenReservationId = 0L;
        Deque<OutputEvent> deque = new ConcurrentLinkedDeque<>();
        OrderPickedUpEvent orderPickedUpEventStub = OrderPickedUpEvent
                .of(10000L, CourierArrivedEvent.of(courierId, 250, 9800L),
                        OrderPreparedEvent.of(kitchenReservationId, "order-id", 900L), false, 500L);
        OrderDeliveredEvent orderDeliveredEvent = OrderDeliveredEvent.of(orderPickedUpEventStub);

        //#setup courier
        Mockito.doNothing().when(courierServiceMock).registerNotificationDeque(deque);
        Mockito.doNothing().when(courierServiceMock).shutdown();

        //#setup kitchen
        Mockito.doNothing().when(kitchenServiceMock).registerNotificationDeque(deque);
        Mockito.doNothing().when(kitchenServiceMock).shutdown();

        //#setup handler
        Mockito.doNothing().when(outputEventPublisherMock).registerNotificationDeque(deque);
        underTest = new OrderProcessor(config, courierServiceMock, kitchenServiceMock, outputEventPublisherMock,
                deque, this.scheduler);

        //when
        MetricsProcessor.DeliveryMetricsSnapshot metricsSnapshot = underTest.getMetricsSnapshot();
        CompletableFuture<Void> completedHandle = underTest.getCompletedHandle();
        completedHandle.join();

        //then
        Assertions.assertThat(completedHandle.isDone()).isTrue();
        //verify mocks
        Mockito.verify(courierServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(courierServiceMock, Mockito.times(1)).shutdown();
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).shutdown();
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(outputEventPublisherMock, Mockito.times(1)).registerNotificationDeque(deque);
    }
}