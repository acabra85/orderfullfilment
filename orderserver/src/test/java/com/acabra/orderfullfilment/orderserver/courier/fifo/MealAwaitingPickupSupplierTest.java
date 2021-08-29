package com.acabra.orderfullfilment.orderserver.courier.fifo;

import com.acabra.orderfullfilment.orderserver.courier.event.CourierReadyForPickupEvent;
import com.acabra.orderfullfilment.orderserver.courier.event.PickupCompletedEvent;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.*;
import java.util.function.Supplier;

@Slf4j
class MealAwaitingPickupSupplierTest {

    MealAwaitingPickupSupplier underTest;

    @AfterEach
    public void setup() {
        underTest = null;
    }

    @Test
    public void shouldWaitRetrieveValidEvent_fromTheEmptyDeque() throws InterruptedException, ExecutionException {
        //given
        final LinkedBlockingDeque<CourierReadyForPickupEvent> queue = new LinkedBlockingDeque<>();
        long readySince = 3456L;
        underTest = new MealAwaitingPickupSupplier(queue, readySince);
        CompletableFuture<PickupCompletedEvent> future = CompletableFuture.supplyAsync(underTest);
        Assertions.assertThat(queue.isEmpty()).isTrue();
        final CourierReadyForPickupEvent event = new CourierReadyForPickupEvent(952161, 11234L, 6454L);

        //when
        CompletableFuture.runAsync(() -> {
            try {
                queue.put(event);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, CompletableFuture.delayedExecutor(100, TimeUnit.MILLISECONDS));

        //then
        PickupCompletedEvent actual = future.get();
        Assertions.assertThat(actual).isNotNull();
        Assertions.assertThat(actual.courierId).isEqualTo(event.courierId);
        Assertions.assertThat(actual.courierWaitTime).isEqualTo(actual.at-event.arrivalTime);
        Assertions.assertThat(actual.foodWaitTime).isEqualTo(actual.at-readySince);
    }

    @Test
    public void shouldRetrieveValidEvent_fromANonEmptyQueue() throws InterruptedException, ExecutionException {
        //given
        final CourierReadyForPickupEvent event = new CourierReadyForPickupEvent(952161, 11234L, 6454L);
        final LinkedBlockingDeque<CourierReadyForPickupEvent> queue = new LinkedBlockingDeque<>() {{
            add(event);
        }};
        Assertions.assertThat(queue.isEmpty()).isFalse();
        long readySince = 3456L;
        underTest = new MealAwaitingPickupSupplier(queue, readySince);
        CompletableFuture<PickupCompletedEvent> future = CompletableFuture.supplyAsync(underTest,
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
        PickupCompletedEvent actual = future.get();
        Assertions.assertThat(actual).isNotNull();
        Assertions.assertThat(actual.courierId).isEqualTo(event.courierId);
        Assertions.assertThat(actual.courierWaitTime).isEqualTo(actual.at-event.arrivalTime);
        Assertions.assertThat(actual.foodWaitTime).isEqualTo(actual.at-readySince);
    }

    @Test
    public void shouldReturnNull_whenExceptionIsThrown() throws InterruptedException {
        //given
        LinkedBlockingDeque<CourierReadyForPickupEvent> queue = Mockito.mock(LinkedBlockingDeque.class);
        Mockito.doThrow(InterruptedException.class).when(queue).take();
        long readySince = 3456L;
        underTest = new MealAwaitingPickupSupplier(queue, readySince);

        //when
        PickupCompletedEvent actual = underTest.get();

        //then
        Mockito.verify(queue).take();
        Assertions.assertThat(actual).isNull();
    }
}