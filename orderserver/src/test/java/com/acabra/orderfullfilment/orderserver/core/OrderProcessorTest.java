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

import java.util.Optional;
import java.util.concurrent.*;


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
        BlockingDeque<OutputEvent> deque = new LinkedBlockingDeque<>();
        underTest = new OrderProcessor(config, courierServiceMock, kitchenServiceMock, outputEventPublisherMock, deque);
        OrderPreparedEvent orderPreparedEventStub = OrderPreparedEvent.of(1, bananaSplitOrder.id, 1000);

        //#setup courier
        Mockito.doNothing().when(courierServiceMock).registerNotificationDeque(deque);
        Mockito.when(courierServiceMock.processOrderPrepared(orderPreparedEventStub))
                .thenReturn(CompletableFuture.completedFuture(null));

        //#setup kitchen
        Mockito.doNothing().when(kitchenServiceMock).registerNotificationDeque(deque);

        //#setup handler
        Mockito.doNothing().when(outputEventPublisherMock).registerNotificationDeque(deque);

        //when
        deque.offer(orderPreparedEventStub); //send the event on the queue for processing
        awaitTermination().await();
        underTest.close();

        //then

        //verify mocks
        Mockito.verify(courierServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(courierServiceMock, Mockito.times(1)).processOrderPrepared(orderPreparedEventStub);
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(outputEventPublisherMock, Mockito.times(1)).registerNotificationDeque(deque);
    }

    @Test
    void mustProcessOrderReceivedEvent_givenThatCourierDispatchedAndKitchenAcceptsReservation() throws InterruptedException {
        //given
        Integer courierId = 1;
        long cookReservationId = 0L;
        BlockingDeque<OutputEvent> deque = new LinkedBlockingDeque<>();
        underTest = new OrderProcessor(config, courierServiceMock, kitchenServiceMock, outputEventPublisherMock, deque);
        OrderReceivedEvent orderReceivedEvent = OrderReceivedEvent.of(5, bananaSplitOrder);

        //#setup courier
        Mockito.doNothing()
                .when(courierServiceMock).registerNotificationDeque(deque);
        Mockito.doReturn(Optional.of(courierId))
                .when(courierServiceMock).dispatchRequest(Mockito.any());

        //#setup kitchen
        Mockito.doNothing()
                .when(kitchenServiceMock).registerNotificationDeque(deque);
        Mockito.doReturn(cookReservationId)
                .when(kitchenServiceMock).orderCookReservationId(bananaSplitOrder);
        Mockito.doReturn(CompletableFuture.completedFuture(true))
                .when(kitchenServiceMock).prepareMeal(cookReservationId);

        //#setup handler
        Mockito.doNothing().when(outputEventPublisherMock).registerNotificationDeque(deque);

        //when
        deque.offer(orderReceivedEvent);
        awaitTermination().await();
        underTest.close();

        //then

        //verify mocks
        Mockito.verify(courierServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(courierServiceMock, Mockito.times(1)).dispatchRequest(bananaSplitOrder);
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).orderCookReservationId(bananaSplitOrder);
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).prepareMeal(cookReservationId);
        Mockito.verify(outputEventPublisherMock, Mockito.times(1)).registerNotificationDeque(deque);
    }

    @Test
    void mustProcessOrderReceivedEvent_givenThatNoCouriersAvailableAndKitchenAcceptsReservation() throws InterruptedException {
        //given
        long cookReservationId = 0L;
        BlockingDeque<OutputEvent> deque = new LinkedBlockingDeque<>();
        underTest = new OrderProcessor(config, courierServiceMock, kitchenServiceMock, outputEventPublisherMock, deque);
        OrderReceivedEvent orderReceivedEvent = OrderReceivedEvent.of(5, bananaSplitOrder);

        //#setup courier
        Mockito.doNothing()
                .when(courierServiceMock).registerNotificationDeque(deque);
        Mockito.doReturn(Optional.empty())
                .when(courierServiceMock).dispatchRequest(bananaSplitOrder);

        //#setup kitchen
        Mockito.doNothing()
                .when(kitchenServiceMock).registerNotificationDeque(deque);
        Mockito.doReturn(cookReservationId)
                .when(kitchenServiceMock).orderCookReservationId(bananaSplitOrder);
        Mockito.doReturn(true)
                .when(kitchenServiceMock).cancelCookReservation(cookReservationId);

        //#setup handler
        Mockito.doNothing().when(outputEventPublisherMock).registerNotificationDeque(deque);

        //when
        deque.offer(orderReceivedEvent);
        awaitTermination().await();
        underTest.close();
        //then

        //verify mocks
        Mockito.verify(courierServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(courierServiceMock, Mockito.times(1)).dispatchRequest(bananaSplitOrder);
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).orderCookReservationId(bananaSplitOrder);
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).cancelCookReservation(cookReservationId);
        Mockito.verify(outputEventPublisherMock, Mockito.times(1)).registerNotificationDeque(deque);
    }

    @Test
    void mustProcessOrderPickedUpEvent_andDeliveryEvent_instantDelivery() throws InterruptedException {
        //given
        int courierId = 1;
        long cookReservationId = 0L;
        BlockingDeque<OutputEvent> deque = new LinkedBlockingDeque<>();
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

        awaitTermination().await();
        underTest.close();
        //then

        //verify mocks
        Mockito.verify(courierServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(kitchenServiceMock, Mockito.times(1)).registerNotificationDeque(deque);
        Mockito.verify(outputEventPublisherMock, Mockito.times(1)).registerNotificationDeque(deque);
    }

    private CountDownLatch awaitTermination() {
        CountDownLatch lock = new CountDownLatch(1);
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Assertions.fail("");
            } finally {
                lock.countDown();
            }
        });
        return lock;
    }
}