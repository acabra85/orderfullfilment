package com.acabra.orderfullfilment.orderserver.courier.fifo;

import com.acabra.orderfullfilment.orderserver.courier.CourierFleet;
import com.acabra.orderfullfilment.orderserver.courier.CourierFleetImpl;
import com.acabra.orderfullfilment.orderserver.courier.event.CourierReadyForPickupEvent;
import com.acabra.orderfullfilment.orderserver.kitchen.event.MealReadyForPickupEvent;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;

class CourierDispatchFIFOServiceImplTest {

    private CourierDispatchFIFOServiceImpl underTest;
    private final CourierFleet fleetMock = Mockito.mock(CourierFleetImpl.class);
    private final CourierReadyForPickupEvent validCourierReadyEvent = new CourierReadyForPickupEvent(1, 1, 1);
    private final MealReadyForPickupEvent validMealEvent = MealReadyForPickupEvent.of(3213, "dasdsa", 3123123);

    @BeforeEach
    public void setup() {
        underTest = new CourierDispatchFIFOServiceImpl(fleetMock);
    }

    @Test
    void mustReturnEmptyNoCouriers() {
        //given
        Mockito.when(fleetMock.dispatch(null)).thenReturn(null);

        //when
        Optional actual = underTest.dispatchRequest(null);

        //then
        Mockito.verify(fleetMock).dispatch(null);
        Assertions.assertThat(actual.isEmpty()).isTrue();
    }

    @Test
    void mustReturnCourierIdAvailableCourier() {
        //given
        Mockito.when(fleetMock.dispatch(null)).thenReturn(5);

        //when
        Optional actual = underTest.dispatchRequest(null);

        //then
        Mockito.verify(fleetMock).dispatch(null);
        Assertions.assertThat(actual.get()).isEqualTo(5);
    }

    @Test
    void mustComplete_whenCouriersAvailableForPickup() throws InterruptedException, ExecutionException {
        //given
        LinkedBlockingDeque<CourierReadyForPickupEvent> mockDeque = Mockito.mock(LinkedBlockingDeque.class);
        underTest = new CourierDispatchFIFOServiceImpl(fleetMock, mockDeque);
        Mockito.when(mockDeque.take()).thenReturn(validCourierReadyEvent);
        Mockito.doNothing().when(fleetMock).release(validCourierReadyEvent.courierId);

        CompletableFuture<Void> future = underTest.processMealReady(validMealEvent);
        //when
        future.get();

        //then
        Mockito.verify(mockDeque).take();
        Mockito.verify(fleetMock).release(validCourierReadyEvent.courierId);
        Assertions.assertThat(future.isDone()).isTrue();
    }

    @Test
    void mustComplete_whenExceptionRaised() throws InterruptedException, ExecutionException {
        //given
        LinkedBlockingDeque<CourierReadyForPickupEvent> mockDeque = Mockito.mock(LinkedBlockingDeque.class);
        Mockito.doThrow(InterruptedException.class).when(mockDeque).take();
        underTest = new CourierDispatchFIFOServiceImpl(fleetMock, mockDeque);

        //when
        CompletableFuture<Void> future = underTest.processMealReady(validMealEvent);
        future.get();

        //then
        Mockito.verify(mockDeque).take();
        Assertions.assertThat(future.isDone()).isTrue();
    }
}