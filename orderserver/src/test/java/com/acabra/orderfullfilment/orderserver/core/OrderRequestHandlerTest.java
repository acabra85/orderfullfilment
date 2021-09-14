package com.acabra.orderfullfilment.orderserver.core;

import com.acabra.orderfullfilment.orderserver.dto.DeliveryOrderRequestDTO;
import com.acabra.orderfullfilment.orderserver.event.OrderReceivedEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;
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
    void mustRegisterAndPublish() {
        //given
        Deque<OutputEvent> deque = new ConcurrentLinkedDeque<>();
        underTest.registerNotificationDeque(deque);
        underTest.accept(requestStub);

        //when
        OrderReceivedEvent actual = (OrderReceivedEvent) deque.poll();

        //then
        Assertions.assertThat(actual).isNotNull();
        Assertions.assertThat(actual.order).isEqualTo(orderStub);
    }

    @Test
    void mustFailToPublish_givenNoDequeRegistered() {
        //given
        Deque<OutputEvent> deque = new ConcurrentLinkedDeque<>();

        //when
        underTest.accept(requestStub);

        CompletableFuture<OutputEvent> actual = CompletableFuture.supplyAsync(deque::poll,
                CompletableFuture.delayedExecutor(200L, TimeUnit.MILLISECONDS));
        //then
        Assertions.assertThat(actual.join()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void mustFailToPublish_givenDequeReportsException() {
        //given
        Deque<OutputEvent> dequeMock = Mockito.mock(ConcurrentLinkedDeque.class);
        Mockito.doThrow(RuntimeException.class).when(dequeMock).offer(Mockito.any(OutputEvent.class));
        underTest.registerNotificationDeque(dequeMock);

        //when
        underTest.accept(requestStub);

        //then
        Mockito.verify(dequeMock, Mockito.times(1)).offer(Mockito.any(OutputEvent.class));
    }

    @Test
    void mustNotPublishEvent_givenDeliveryRequestInvalid() {
        //given
        DeliveryOrderRequestDTO invalid = new DeliveryOrderRequestDTO("", "", -1);
        Deque<OutputEvent> deque = new ConcurrentLinkedDeque<>();
        underTest.registerNotificationDeque(deque);

        //when
        underTest.accept(invalid);

        CompletableFuture<OutputEvent> actual = CompletableFuture.supplyAsync(deque::poll,
                CompletableFuture.delayedExecutor(200L, TimeUnit.MILLISECONDS));
        //then
        Assertions.assertThat(actual.join()).isNull();
    }
}