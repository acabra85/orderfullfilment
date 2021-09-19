package com.acabra.orderfullfilment.orderserver.kitchen;

import com.acabra.orderfullfilment.orderserver.config.OrderServerConfig;
import com.acabra.orderfullfilment.orderserver.core.executor.SchedulerExecutorAssistant;
import com.acabra.orderfullfilment.orderserver.event.OrderPreparedEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Deque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ConcurrentLinkedDeque;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {OrderServerConfig.class})
@TestPropertySource(value = "classpath:application.properties")
class KitchenServiceImplTest {

    private KitchenService underTest;
    private final DeliveryOrder deliveryStub = DeliveryOrder.of("id-order-stub", "banana-split", 3);
    private Deque<OutputEvent> mockDeque;
    private final OrderServerConfig config;

    KitchenServiceImplTest(@Autowired OrderServerConfig config) {
        this.config = config;
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        underTest = new KitchenServiceImpl(new SchedulerExecutorAssistant(this.config));
        mockDeque = Mockito.mock(ConcurrentLinkedDeque.class);
    }

    @AfterEach
    public void tearDown() {
        underTest.shutdown();
    }

    @Test
    void mustReturnTrue_cancelReservationWithValidNumber() {
        //given
        long reservationId = underTest.provideReservationId(deliveryStub);

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
    void notificationSentAfterPrepareMeal() throws InterruptedException {
        //given
        Mockito.doReturn(true).when(mockDeque).offer(Mockito.any(OrderPreparedEvent.class));

        underTest.registerNotificationDeque(mockDeque);
        long reservationId = underTest.provideReservationId(deliveryStub);
        Assertions.assertThat(underTest.isKitchenIdle()).isTrue();

        //when
        CompletableFuture<Boolean> cookHandle = underTest.prepareMeal(reservationId);
        Assertions.assertThat(underTest.isKitchenIdle()).isFalse();

        Thread.sleep(2000L);
        cookHandle.join();

        //then
        Mockito.verify(mockDeque, Mockito.times(1)).offer(Mockito.any(OrderPreparedEvent.class));
        Assertions.assertThat(cookHandle.isDone()).isTrue();
        Assertions.assertThat(underTest.isKitchenIdle()).isTrue();
    }

    @Test
    void notificationMissedAfterPrepareMeal_exceptionThrown() {
        //given
        Mockito.doThrow(RuntimeException.class).when(mockDeque).offer(Mockito.any(OrderPreparedEvent.class));
        underTest.registerNotificationDeque(mockDeque);
        long reservationId = underTest.provideReservationId(deliveryStub);
        Assertions.assertThat(underTest.isKitchenIdle()).isTrue();

        //when
        CompletableFuture<Boolean> cookHandle = underTest.prepareMeal(reservationId);
        Assertions.assertThat(underTest.isKitchenIdle()).isFalse();
        Boolean result = cookHandle.join();

        //then
        Assertions.assertThat(result).isFalse();
        Mockito.verify(mockDeque, Mockito.times(1)).offer(Mockito.any(OrderPreparedEvent.class));
        Assertions.assertThat(underTest.isKitchenIdle()).isTrue();
    }

    @Test
    void notificationMissedAfterPrepareMeal_interruptedDequeOperation() {
        //given
        Mockito.doThrow(RuntimeException.class).when(mockDeque).offer(Mockito.any(OrderPreparedEvent.class));
        underTest.registerNotificationDeque(mockDeque);
        long reservationId = underTest.provideReservationId(deliveryStub);
        Assertions.assertThat(underTest.isKitchenIdle()).isTrue();

        //when
        CompletableFuture<Boolean> cookHandle = underTest.prepareMeal(reservationId);
        Assertions.assertThat(underTest.isKitchenIdle()).isFalse();
        Boolean result = cookHandle.join();

        //then
        Mockito.verify(mockDeque, Mockito.times(1)).offer(Mockito.any(OrderPreparedEvent.class));
        Assertions.assertThat(result).isFalse();
        Assertions.assertThat(underTest.isKitchenIdle()).isTrue();
    }

    @Test
    void notificationMissedAfterPrepareMeal_noQueueAvailableForNotification() throws ExecutionException, InterruptedException {
        //given
        long reservationId = underTest.provideReservationId(deliveryStub);
        Assertions.assertThat(underTest.isKitchenIdle()).isTrue();

        //when
        CompletableFuture<Boolean> cookHandle = underTest.prepareMeal(reservationId);
        Assertions.assertThat(underTest.isKitchenIdle()).isFalse();
        cookHandle.get();

        //then
        Mockito.verifyNoInteractions(mockDeque);
        Assertions.assertThat(cookHandle.isCompletedExceptionally()).isFalse();
        Assertions.assertThat(underTest.isKitchenIdle()).isTrue();
    }
}