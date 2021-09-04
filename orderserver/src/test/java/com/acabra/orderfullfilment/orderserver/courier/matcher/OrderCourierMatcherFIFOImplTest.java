package com.acabra.orderfullfilment.orderserver.courier.matcher;

import com.acabra.orderfullfilment.orderserver.event.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Deque;
import java.util.concurrent.*;

class OrderCourierMatcherFIFOImplTest {

    private OrderCourierMatcherFIFOImpl underTest;

    @BeforeEach
    public void setup() {
        underTest = new OrderCourierMatcherFIFOImpl();
    }

    @Test
    public void mustMatchMealAndCourier_givenCourierArrivesAndNoMealsAwaiting() throws ExecutionException, InterruptedException {
        //given
        Deque<OutputEvent> queue = new ConcurrentLinkedDeque<>();
        underTest.registerNotificationDeque(queue);

        CompletableFuture<OutputEvent> completionFuture = CompletableFuture.supplyAsync(() -> {
                while (queue.peek() == null) {
                }
                return queue.poll();
                }, CompletableFuture.delayedExecutor(50, TimeUnit.MILLISECONDS));

        CompletableFuture.runAsync(() -> underTest.acceptMealPreparedEvent(OrderPreparedEvent.of(1, "meal-id", 80)),
                CompletableFuture.delayedExecutor(30, TimeUnit.MILLISECONDS));

        //when
        underTest.acceptCourierArrivedEvent(CourierArrivedEvent.of(1, 100, 101));

        //then
        OutputEvent outputEvent = completionFuture.get();
        Assertions.assertThat(outputEvent).isNotNull();
        Assertions.assertThat(outputEvent.type).isEqualTo(EventType.ORDER_PICKED_UP);
    }

    @Test
    public void mustMatchOrderAndCourier_givenOrderPreparedNoCouriersWaiting() throws ExecutionException, InterruptedException {
        //given
        Deque<OutputEvent> queue = new ConcurrentLinkedDeque<>();
        underTest.registerNotificationDeque(queue);
        CompletableFuture<OutputEvent> completionFuture = CompletableFuture.supplyAsync(() -> {
            while (queue.peek() == null) {
            }
            return queue.poll();
        }, CompletableFuture.delayedExecutor(50, TimeUnit.MILLISECONDS));
        CompletableFuture.runAsync(() ->
                        underTest.acceptCourierArrivedEvent(CourierArrivedEvent.of(1, 100, 101)),
                CompletableFuture.delayedExecutor(30, TimeUnit.MILLISECONDS));

        //when
        underTest.acceptMealPreparedEvent(OrderPreparedEvent.of(1, "meal-id", 80));

        //then
        OutputEvent outputEvent = completionFuture.get();
        Assertions.assertThat(outputEvent).isNotNull();
        Assertions.assertThat(outputEvent.type).isEqualTo(EventType.ORDER_PICKED_UP);
    }

    @Test
    public void mustReturnNull_publicNotificationQueueNotRegistered() {
        //given
        Deque<OutputEvent> queue = new ConcurrentLinkedDeque<>();
        CompletableFuture<OutputEvent> completionFuture = CompletableFuture.supplyAsync(() -> {
            while (queue.peek() == null) {
            }
            return queue.poll();
        }, CompletableFuture.delayedExecutor(50, TimeUnit.MILLISECONDS));
        CompletableFuture.runAsync(() ->
                        underTest.acceptCourierArrivedEvent(CourierArrivedEvent.of(1, 100, 101)),
                CompletableFuture.delayedExecutor(30, TimeUnit.MILLISECONDS));

        //when
        underTest.acceptMealPreparedEvent(OrderPreparedEvent.of(1, "meal-id", 80));

        //then
        OutputEvent outputEvent = completionFuture.getNow(null);
        Assertions.assertThat(outputEvent).isNull();
    }

    @Test
    public void mustReturnNull_exceptionThrownWhilePublishing() throws InterruptedException {
        //given
        Deque<OutputEvent> queueMock = Mockito.mock(ConcurrentLinkedDeque.class);
        Mockito.doThrow(RuntimeException.class).when(queueMock).offer(Mockito.any(OutputEvent.class));
        underTest.registerNotificationDeque(queueMock);
        CompletableFuture<OutputEvent> completionFuture = CompletableFuture
                .supplyAsync(() -> queueMock.poll(), CompletableFuture.delayedExecutor(50, TimeUnit.MILLISECONDS));
        CompletableFuture.runAsync(() ->
                underTest.acceptCourierArrivedEvent(CourierArrivedEvent.of(1, 100, 101)),
            CompletableFuture.delayedExecutor(30, TimeUnit.MILLISECONDS));

        //when
        underTest.acceptMealPreparedEvent(OrderPreparedEvent.of(1, "meal-id", 80));

        //then
        OutputEvent outputEvent = completionFuture.getNow(null);
        Assertions.assertThat(outputEvent).isNull();
    }
}