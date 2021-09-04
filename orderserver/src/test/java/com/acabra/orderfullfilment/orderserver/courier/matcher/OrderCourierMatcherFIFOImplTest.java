package com.acabra.orderfullfilment.orderserver.courier.matcher;

import com.acabra.orderfullfilment.orderserver.event.*;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

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
        BlockingDeque<OutputEvent> queue = new LinkedBlockingDeque<>();
        underTest.registerNotificationDeque(queue);
        CompletableFuture<OutputEvent> completionFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        });
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
        BlockingDeque<OutputEvent> queue = new LinkedBlockingDeque<>();
        underTest.registerNotificationDeque(queue);
        CompletableFuture<OutputEvent> completionFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        });
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
        BlockingDeque<OutputEvent> queue = new LinkedBlockingDeque<>();
        CompletableFuture<OutputEvent> completionFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return queue.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        });
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
        BlockingDeque<OutputEvent> queueMock = Mockito.mock(LinkedBlockingDeque.class);
        Mockito.doThrow(InterruptedException.class).when(queueMock).offer(Mockito.any(OutputEvent.class));
        underTest.registerNotificationDeque(queueMock);
        CompletableFuture<OutputEvent> completionFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return queueMock.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        });
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