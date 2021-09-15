package com.acabra.orderfullfilment.orderserver.core.executor;

import com.acabra.orderfullfilment.orderserver.event.EventType;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayDeque;
import java.util.function.Consumer;

@SuppressWarnings("unchecked")
class OutputEventHandlerTest {

    private OutputEventHandler underTest;
    private final Consumer<OutputEvent> consumerMock = Mockito.mock(Consumer.class);
    private final ArrayDeque<OutputEvent> deque = new ArrayDeque<>();

    @BeforeEach
    public void setup() {
        underTest = new OutputEventHandler(deque, consumerMock);
    }

    @Test
    public void mustCallTheConsumer_givenElementsAreAvailableInQueue() {
        //given
        OutputEvent event = new OutputEvent(EventType.ORDER_RECEIVED, 0L) {};
        Mockito.doNothing().when(consumerMock).accept(event);

        //when
        underTest.doWork(); // no elements available
        deque.offer(event);
        underTest.doWork(); //element available
        underTest.doWork(); //no elements available
        underTest.doWork();
        underTest.doWork();
        underTest.doWork();

        //then
        Mockito.verify(consumerMock, Mockito.times(1)).accept(event);
        Assertions.assertThat(deque.isEmpty()).isTrue();
    }


}