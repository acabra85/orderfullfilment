package com.acabra.orderfullfilment.orderserver.core;

import com.acabra.orderfullfilment.orderserver.dto.DeliveryOrderRequestDTO;
import com.acabra.orderfullfilment.orderserver.event.OrderReceivedEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

class OrderRequestHandlerTest {

    private OrderRequestHandler underTest;

    private final DeliveryOrder orderStub = DeliveryOrder.of("order-id", "banana_split", 5);
    private final DeliveryOrderRequestDTO requestStub = new DeliveryOrderRequestDTO(orderStub.id, orderStub.name, orderStub.prepTime);

    @BeforeEach
    public void setup() {
        this.underTest = new OrderRequestHandler();
    }

    @Test
    void mustRegisterAndPublish() throws InterruptedException {
        //given
        BlockingDeque<OutputEvent> deque = new LinkedBlockingDeque<>();
        underTest.registerNotificationDeque(deque);
        underTest.accept(requestStub);

        //when
        OrderReceivedEvent actual = (OrderReceivedEvent) deque.take();

        //then
        Assertions.assertThat(actual.order).isEqualTo(orderStub);
    }

    @Test
    void mustFailToPublish_givenNoDequeRegistered() throws InterruptedException {
        //given
        BlockingDeque<OutputEvent> deque = new LinkedBlockingDeque<>();

        //when
        underTest.accept(requestStub);

        //then
        Assertions.assertThat(deque.poll(200L, TimeUnit.MILLISECONDS)).isNull();
    }

    @Test
    void mustFailToPublish_givenDequeReportsException() throws InterruptedException {
        //given
        BlockingDeque<OutputEvent> dequeMock = Mockito.mock(LinkedBlockingDeque.class);
        Mockito.doThrow(InterruptedException.class).when(dequeMock).put(Mockito.any(OutputEvent.class));
        underTest.registerNotificationDeque(dequeMock);

        //when
        underTest.accept(requestStub);

        //then
        Mockito.verify(dequeMock, Mockito.times(1)).put(Mockito.any(OutputEvent.class));
    }

    @Test
    void mustNotPublishEvent_givenDeliveryRequestInvalid() throws InterruptedException {
        //given
        DeliveryOrderRequestDTO invalid = new DeliveryOrderRequestDTO("", "", -1);
        BlockingDeque<OutputEvent> deque = new LinkedBlockingDeque<>();
        underTest.registerNotificationDeque(deque);

        //when
        underTest.accept(invalid);

        //then
        Assertions.assertThat(deque.poll(200L, TimeUnit.MILLISECONDS)).isNull();
    }
}