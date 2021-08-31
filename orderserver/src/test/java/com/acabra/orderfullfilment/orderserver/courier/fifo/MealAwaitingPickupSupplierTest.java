package com.acabra.orderfullfilment.orderserver.courier.fifo;

import com.acabra.orderfullfilment.orderserver.event.CourierArrivedEvent;
import com.acabra.orderfullfilment.orderserver.event.OrderPickedUpEvent;
import com.acabra.orderfullfilment.orderserver.event.OrderPreparedEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.*;

@Slf4j
class MealAwaitingPickupSupplierTest {

    MealAwaitingPickupSupplier underTest;
    private OrderPreparedEvent stubEvent = OrderPreparedEvent.of(1, "some-order-id", 3456L);

    @AfterEach
    public void setup() {
        underTest = null;
    }

    @Test
    public void shouldWaitRetrieveValidEvent_fromTheEmptyDeque() throws InterruptedException, ExecutionException {
        //given
        final LinkedBlockingDeque<OutputEvent> queue = new LinkedBlockingDeque<>();
        long readySince = stubEvent.createdAt;
        underTest = MealAwaitingPickupSupplier.of(queue, stubEvent);
        CompletableFuture<OrderPickedUpEvent> future = CompletableFuture.supplyAsync(underTest);
        Assertions.assertThat(queue.isEmpty()).isTrue();
        final CourierArrivedEvent event = CourierArrivedEvent.of(952161, 11234L, 6454L);

        //when
        CompletableFuture.runAsync(() -> {
            try {
                queue.put(event);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS));

        //then
        OrderPickedUpEvent actual = future.get();
        Assertions.assertThat(actual).isNotNull();
        Assertions.assertThat(actual.courierId).isEqualTo(event.courierId);
        Assertions.assertThat(actual.courierWaitTime).isEqualTo(actual.createdAt-event.createdAt);
        Assertions.assertThat(actual.foodWaitTime).isEqualTo(actual.createdAt-readySince);
    }

    @Test
    public void shouldRetrieveValidEvent_fromANonEmptyQueue() throws InterruptedException, ExecutionException {
        //given
        final CourierArrivedEvent event = CourierArrivedEvent.of(952161, 11234L, 6454L);
        final LinkedBlockingDeque<OutputEvent> queue = new LinkedBlockingDeque<>() {{
            add(event);
        }};
        Assertions.assertThat(queue.isEmpty()).isFalse();
        long readySince = stubEvent.createdAt;
        underTest = MealAwaitingPickupSupplier.of(queue, stubEvent);
        CompletableFuture<OrderPickedUpEvent> future = CompletableFuture.supplyAsync(underTest,
                CompletableFuture.delayedExecutor(10, TimeUnit.MILLISECONDS));

        //when
        CompletableFuture.runAsync(() -> {
            try {
                queue.put(event);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS));

        //then
        OrderPickedUpEvent actual = future.get();
        Assertions.assertThat(actual).isNotNull();
        Assertions.assertThat(actual.courierId).isEqualTo(event.courierId);
        Assertions.assertThat(actual.courierWaitTime).isEqualTo(actual.createdAt-event.createdAt);
        Assertions.assertThat(actual.foodWaitTime).isEqualTo(actual.createdAt-readySince);
    }

    @Test
    public void shouldReturnNull_whenExceptionIsThrown() throws InterruptedException {
        //given
        LinkedBlockingDeque<OutputEvent> queue = Mockito.mock(LinkedBlockingDeque.class);
        Mockito.doThrow(InterruptedException.class).when(queue).take();
        long readySince = 3456L;
        underTest = MealAwaitingPickupSupplier.of(queue, stubEvent);

        //when
        OrderPickedUpEvent actual = underTest.get();

        //then
        Mockito.verify(queue).take();
        Assertions.assertThat(actual).isNull();
    }
}