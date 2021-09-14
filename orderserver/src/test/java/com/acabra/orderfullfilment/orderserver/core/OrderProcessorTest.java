package com.acabra.orderfullfilment.orderserver.core;

import com.acabra.orderfullfilment.orderserver.config.OrderServerConfig;
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
import java.util.concurrent.Callable;
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

    OrderProcessorTest(@Autowired OrderServerConfig config) {
        this.config = config;
    }

    @BeforeEach
    public void setup() {
        underTest = null;
        courierServiceMock = Mockito.mock(CourierServiceImpl.class);
        kitchenServiceMock = Mockito.mock(KitchenServiceImpl.class);
        outputEventPublisherMock = Mockito.mock(OrderRequestHandler.class);
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
        underTest = new OrderProcessor(config, courierServiceMock, kitchenServiceMock, outputEventPublisherMock,
                    deque);
        OrderPreparedEvent orderPreparedEventStub = OrderPreparedEvent.of(1, bananaSplitOrder.id, 1000);

        //#setup courier
        Mockito.doNothing().when(courierServiceMock).registerNotificationDeque(deque);
        Mockito.when(courierServiceMock.processOrderPrepared(orderPreparedEventStub))
                .thenReturn(true);

        //#setup kitchen
        Mockito.doNothing().when(kitchenServiceMock).registerNotificationDeque(deque);

        //#setup handler
        Mockito.doNothing().when(outputEventPublisherMock).registerNotificationDeque(deque);

        //when
        deque.offer(orderPreparedEventStub); //send the event on the queue for processing
        CompletableFuture<Void> future = awaitTermination(2000L);
        while(!future.isDone()) {
            Thread.sleep(1000L);
        }
        underTest.close();

        //then

        //verify mocks
        Mockito.verify(courierServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(courierServiceMock, Mockito.times(1)).processOrderPrepared(orderPreparedEventStub);
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(outputEventPublisherMock, Mockito.times(1)).registerNotificationDeque(deque);
    }

    private CompletableFuture<Void> awaitTermination(long millis) {
        return CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(millis);
            }catch (Exception e)  {

            }
        });
    }

    @Test
    void mustProcessOrderReceivedEvent_givenThatCourierDispatchedAndKitchenAcceptsReservation() {
        //given
        Integer courierId = 1;
        long cookReservationId = 0L;
        Deque<OutputEvent> deque = new ConcurrentLinkedDeque<>();
        underTest = new OrderProcessor(config, courierServiceMock, kitchenServiceMock, outputEventPublisherMock, deque);
        OrderReceivedEvent orderReceivedEvent = OrderReceivedEvent.of(5, bananaSplitOrder);

        //#setup courier
        Mockito.doNothing()
                .when(courierServiceMock).registerNotificationDeque(deque);
        Mockito.doReturn(Optional.of(courierId))
                .when(courierServiceMock).dispatchRequest(bananaSplitOrder, cookReservationId);

        //#setup kitchen
        Mockito.doNothing()
                .when(kitchenServiceMock).registerNotificationDeque(deque);
        Mockito.doReturn(cookReservationId)
                .when(kitchenServiceMock).provideReservationId(bananaSplitOrder);
        Mockito.doReturn(CompletableFuture.completedFuture(true))
                .when(kitchenServiceMock).prepareMeal(cookReservationId);

        //#setup handler
        Mockito.doNothing().when(outputEventPublisherMock).registerNotificationDeque(deque);

        //when
        deque.offer(orderReceivedEvent);
        awaitTermination(250L, () -> underTest.getMetricsSnapshot().totalOrdersReceived == 1).join();
        underTest.close();

        //then
        Assertions.assertThat(underTest.getMetricsSnapshot().totalOrdersReceived).isEqualTo(1);

        //verify mocks
        Mockito.verify(courierServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(courierServiceMock, Mockito.times(1)).dispatchRequest(bananaSplitOrder, cookReservationId);
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).provideReservationId(bananaSplitOrder);
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).prepareMeal(cookReservationId);
        Mockito.verify(outputEventPublisherMock, Mockito.times(1)).registerNotificationDeque(deque);
    }

    @Test
    void mustProcessOrderReceivedEvent_givenThatNoCouriersAvailableAndKitchenAcceptsReservation() {
        //given
        long cookReservationId = 0L;
        Deque<OutputEvent> deque = new ConcurrentLinkedDeque<>();
        underTest = new OrderProcessor(config, courierServiceMock, kitchenServiceMock, outputEventPublisherMock, deque);
        OrderReceivedEvent orderReceivedEvent = OrderReceivedEvent.of(5, bananaSplitOrder);

        //#setup courier
        Mockito.doNothing()
                .when(courierServiceMock).registerNotificationDeque(deque);
        Mockito.doReturn(Optional.empty())
                .when(courierServiceMock).dispatchRequest(bananaSplitOrder, cookReservationId);

        //#setup kitchen
        Mockito.doNothing()
                .when(kitchenServiceMock).registerNotificationDeque(deque);
        Mockito.doReturn(cookReservationId)
                .when(kitchenServiceMock).provideReservationId(bananaSplitOrder);
        Mockito.doReturn(true)
                .when(kitchenServiceMock).cancelCookReservation(cookReservationId);

        //#setup handler
        Mockito.doNothing().when(outputEventPublisherMock).registerNotificationDeque(deque);

        //when
        deque.offer(orderReceivedEvent);
        awaitTermination(250, () -> underTest.getMetricsSnapshot().totalOrdersReceived == 1).join();
        underTest.close();

        //verify mocks
        Mockito.verify(courierServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(courierServiceMock, Mockito.times(1)).dispatchRequest(bananaSplitOrder, cookReservationId);
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).provideReservationId(bananaSplitOrder);
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).cancelCookReservation(cookReservationId);
        Mockito.verify(outputEventPublisherMock, Mockito.times(1)).registerNotificationDeque(deque);
    }

    @Test
    void mustProcessOrderPickedUpEvent_andDeliveryEvent_instantDelivery() {
        //given
        int courierId = 1;
        long cookReservationId = 0L;
        Deque<OutputEvent> deque = new ConcurrentLinkedDeque<>();
        underTest = new OrderProcessor(config, courierServiceMock, kitchenServiceMock, outputEventPublisherMock, deque);
        OrderPickedUpEvent orderPickedUpEventStub = OrderPickedUpEvent
                .of(10000L, CourierArrivedEvent.of(courierId, 250, 9800L), 9800L, cookReservationId);

        //#setup courier
        Mockito.doNothing().when(courierServiceMock).registerNotificationDeque(deque);

        //#setup kitchen
        Mockito.doNothing().when(kitchenServiceMock).registerNotificationDeque(deque);

        //#setup handler
        Mockito.doNothing().when(outputEventPublisherMock).registerNotificationDeque(deque);

        //when
        deque.offer(orderPickedUpEventStub);

        //then
        awaitTermination(250L, () -> underTest.getMetricsSnapshot().totalOrdersDelivered == 1).join();
        underTest.close();

        //verify mocks
        Mockito.verify(courierServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(courierServiceMock, Mockito.times(1)).processOrderDelivered(Mockito.any(OrderDeliveredEvent.class));
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(outputEventPublisherMock, Mockito.times(1)).registerNotificationDeque(deque);
    }

    private CompletableFuture<Void> awaitTermination(long millis, Callable<Boolean> exitCondition) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                while(!exitCondition.call()) {
                    Thread.sleep(millis);
                }
            } catch (Exception e) {
                Assertions.fail("");
            }
            return null;
        });
    }
}