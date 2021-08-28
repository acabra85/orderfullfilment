package com.acabra.orderfullfilment.orderserver.courier.fifo;

import com.acabra.orderfullfilment.orderserver.courier.event.CourierReadyForPickupEvent;
import com.acabra.orderfullfilment.orderserver.courier.event.PickupCompletedEvent;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.*;

class MealAwaitingPickupSupplierTest {

    MealAwaitingPickupSupplier underTest;

    @Test
    public void shouldRetrieveValidEvent_whenItsAddedToQueue() throws InterruptedException, ExecutionException {
        //given
        LinkedBlockingDeque<CourierReadyForPickupEvent> queue = new LinkedBlockingDeque<>();
        long readySince = 3456L;
        underTest = new MealAwaitingPickupSupplier(queue, readySince);
        CountDownLatch cl = new CountDownLatch(1);
        CompletableFuture<PickupCompletedEvent> future = CompletableFuture
                .supplyAsync(underTest)
                .handle((evt,ex) ->  {
                    cl.countDown();
                    return evt;
                });
        CourierReadyForPickupEvent event = new CourierReadyForPickupEvent(952161, 11234L, 6454L);

        //when
        queue.add(event);
        cl.await();

        //then
        Assertions.assertThat(future.isDone()).isTrue();
        PickupCompletedEvent actual = future.get();
        Assertions.assertThat(actual).isNotNull();
        Assertions.assertThat(actual.courierWaitTime).isEqualTo(actual.at-event.arrivalTime);
        Assertions.assertThat(actual.foodWaitTime).isEqualTo(actual.at-readySince);
        Assertions.assertThat(actual.courierId).isEqualTo(event.courierId);
    }

    @Test
    public void shouldReturnNull_whenExceptionIsThrown() throws InterruptedException {
        //given
        LinkedBlockingDeque<CourierReadyForPickupEvent> queue = Mockito.mock(LinkedBlockingDeque.class);
        Mockito.doThrow(RuntimeException.class).when(queue).poll();
        long readySince = 3456L;
        underTest = new MealAwaitingPickupSupplier(queue, readySince);
        CountDownLatch cl = new CountDownLatch(1);
        CompletableFuture<PickupCompletedEvent> future = CompletableFuture
                .supplyAsync(underTest)
                .handle((ev,ex) -> {
                    cl.countDown();
                    return ev;
                });

        //when
        cl.await();

        //then
        Mockito.verify(queue).poll();
        Assertions.assertThat(future.isDone()).isTrue();
        Assertions.assertThat(future.isCompletedExceptionally()).isTrue();
    }
}