package com.acabra.orderfullfilment.orderserver.kitchen;

import com.acabra.orderfullfilment.orderserver.event.OrderPreparedEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;

class KitchenServiceImplTest {

    private KitchenService underTest;
    private final DeliveryOrder deliveryStub = DeliveryOrder.of("id-order-stub", "banana-split", 3);
    private BlockingDeque<OutputEvent> mockDeque = Mockito.mock(LinkedBlockingDeque.class);

    @BeforeEach
    void setUp() {
        underTest = new KitchenServiceImpl();
    }

    @Test
    void mustReturnTrue_cancelReservationWithValidNumber() {
        //given
        long reservationId = underTest.orderCookReservationId(deliveryStub);

        //when
        boolean actual = underTest.cancelCookReservation(reservationId);

        //then
        Assertions.assertThat(actual).isTrue();
        Assertions.assertThat(underTest.isKitchenIdle()).isTrue();
    }

    @Test
    void mustReturnFalse_cancelReservationWithInValidNumber() {
        //given
        long invalidId = 999L;

        //when
        boolean reservationCanceled = underTest.cancelCookReservation(invalidId);

        //then
        Assertions.assertThat(reservationCanceled).isFalse();
        Assertions.assertThat(underTest.isKitchenIdle()).isTrue();
    }

    @Test
    void mustFailPrepareMeal_invalidReservationId() {
        //given
        long reservationId = 999L;

        //when
        ThrowableAssert.ThrowingCallable throwsException = () -> underTest.prepareMeal(reservationId).get();

        //then
        Assertions.assertThatThrownBy(throwsException)
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("NoSuchElementException")
                .hasMessageContaining("Unable to find the given cookReservationId")
                .hasMessageContaining("" + reservationId);
        Assertions.assertThat(underTest.isKitchenIdle()).isTrue();

    }

    @Test
    void notificationSentAfterPrepareMeal() throws ExecutionException, InterruptedException {
        //given
        Mockito.doNothing().when(mockDeque).offer(Mockito.any(OrderPreparedEvent.class));

        underTest.registerNotificationDeque(mockDeque);
        long reservationId = underTest.orderCookReservationId(deliveryStub);
        Assertions.assertThat(underTest.isKitchenIdle()).isTrue();

        //when
        CompletableFuture<Boolean> cookHandle = underTest.prepareMeal(reservationId);
        Assertions.assertThat(underTest.isKitchenIdle()).isFalse();

        boolean notificationSent = cookHandle.get();

        //then
        Mockito.verify(mockDeque, Mockito.times(1)).offer(Mockito.any(OrderPreparedEvent.class));
        Assertions.assertThat(notificationSent).isTrue();
        Assertions.assertThat(underTest.isKitchenIdle()).isTrue();
    }

    @Test
    void notificationMissedAfterPrepareMeal_exceptionThrown() throws ExecutionException, InterruptedException {
        //given
        Mockito.doThrow(RuntimeException.class).when(mockDeque).offer(Mockito.any(OrderPreparedEvent.class));
        underTest.registerNotificationDeque(mockDeque);
        long reservationId = underTest.orderCookReservationId(deliveryStub);
        Assertions.assertThat(underTest.isKitchenIdle()).isTrue();

        //when
        CompletableFuture<Boolean> cookHandle = underTest.prepareMeal(reservationId);
        Assertions.assertThat(underTest.isKitchenIdle()).isFalse();
        ThrowableAssert.ThrowingCallable throwsException = () -> cookHandle.get();

        //then
        Assertions.assertThatThrownBy(throwsException)
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("RuntimeException");
        Mockito.verify(mockDeque, Mockito.times(1)).offer(Mockito.any(OrderPreparedEvent.class));
        Assertions.assertThat(underTest.isKitchenIdle()).isTrue();
    }

    @Test
    void notificationMissedAfterPrepareMeal_interruptedDequeOperation() throws ExecutionException, InterruptedException {
        //given
        Mockito.doThrow(InterruptedException.class).when(mockDeque).offer(Mockito.any(OrderPreparedEvent.class));
        underTest.registerNotificationDeque(mockDeque);
        long reservationId = underTest.orderCookReservationId(deliveryStub);
        Assertions.assertThat(underTest.isKitchenIdle()).isTrue();

        //when
        CompletableFuture<Boolean> cookHandle = underTest.prepareMeal(reservationId);
        Assertions.assertThat(underTest.isKitchenIdle()).isFalse();
        Boolean notificationResult = cookHandle.get();

        //then
        Mockito.verify(mockDeque, Mockito.times(1)).offer(Mockito.any(OrderPreparedEvent.class));
        Assertions.assertThat(notificationResult).isFalse();
        Assertions.assertThat(underTest.isKitchenIdle()).isTrue();
    }

    @Test
    void prepareMeal() {
        //given
        long reservationId = underTest.orderCookReservationId(deliveryStub);
        underTest.prepareMeal(reservationId);
    }

    @Test
    void cancelCookReservation() {
    }

    @Test
    void registerMealNotificationReadyQueue() {
    }
}