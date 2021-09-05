package com.acabra.orderfullfilment.orderserver.courier;

import com.acabra.orderfullfilment.orderserver.courier.matcher.OrderCourierMatcherFIFOImpl;
import com.acabra.orderfullfilment.orderserver.courier.matcher.OrderCourierMatcher;
import com.acabra.orderfullfilment.orderserver.courier.model.DispatchResult;
import com.acabra.orderfullfilment.orderserver.event.CourierArrivedEvent;
import com.acabra.orderfullfilment.orderserver.event.OrderDeliveredEvent;
import com.acabra.orderfullfilment.orderserver.event.OrderPreparedEvent;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ConcurrentLinkedDeque;

class CourierServiceImplTest {

    private CourierServiceImpl underTest;
    private final CourierArrivedEvent VALID_COURIER_ARRIVED_EVENT = CourierArrivedEvent.of(1, 1, 1);
    private final OrderPreparedEvent VALID_ORDER_PREPARED_EVENT = OrderPreparedEvent.of(3213, "dasdsa", 3123123);

    private CourierFleet fleetMock;
    private OrderCourierMatcher orderCourierMatcherMock;
    private Deque<OutputEvent> mockDeque;

    @BeforeEach
    public void setup() {
        fleetMock = Mockito.mock(CourierFleetImpl.class);
        orderCourierMatcherMock = Mockito.mock(OrderCourierMatcherFIFOImpl.class);
        mockDeque = Mockito.mock(ConcurrentLinkedDeque.class);
        underTest = new CourierServiceImpl(fleetMock, orderCourierMatcherMock);
    }

    @AfterEach
    public void tearDown() {
        Mockito.verifyNoMoreInteractions(mockDeque, fleetMock, orderCourierMatcherMock);
        mockDeque = null;
        fleetMock = null;
        orderCourierMatcherMock = null;
        underTest = null;
    }

    @Test
    void mustReturnEmptyNoCouriers() {
        //given
        Mockito.when(fleetMock.dispatch(null)).thenReturn(DispatchResult.notDispatched());

        //when
        Optional actual = underTest.dispatchRequest(null);

        //then
        Mockito.verify(fleetMock).dispatch(null);
        Assertions.assertThat(actual.isEmpty()).isTrue();
    }

    @Test
    void mustReturnCourierIdAvailableCourier() {
        //given
        Mockito.when(fleetMock.dispatch(null)).thenReturn(DispatchResult.ofCompleted(5, 1));

        //when
        Optional<Integer> actual = underTest.dispatchRequest(null);

        //then
        Mockito.verify(fleetMock).dispatch(null);
        Assertions.assertThat(actual.isPresent()).isTrue();
        Assertions.assertThat(actual.get()).isEqualTo(5);
    }

    @Test
    void mustComplete_whenCouriersAvailableForPickup() throws InterruptedException, ExecutionException {
        //given
        underTest = new CourierServiceImpl(fleetMock, orderCourierMatcherMock);
        Mockito.doReturn(true)
                .when(orderCourierMatcherMock)
                .acceptMealPreparedEvent(VALID_ORDER_PREPARED_EVENT);
        Mockito.doNothing()
                .when(orderCourierMatcherMock)
                .registerNotificationDeque(mockDeque);
        Mockito.doNothing()
                .when(fleetMock)
                .registerNotificationDeque(mockDeque);
        underTest.registerNotificationDeque(mockDeque);

        CompletableFuture<Boolean> future = underTest.processOrderPrepared(VALID_ORDER_PREPARED_EVENT);
        //when
        boolean actual = future.get();

        //then
        Mockito.verifyNoInteractions(mockDeque);
        Mockito.verify(fleetMock, Mockito.times(1)).registerNotificationDeque(mockDeque);
        Mockito.verify(orderCourierMatcherMock, Mockito.times(1)).acceptMealPreparedEvent(VALID_ORDER_PREPARED_EVENT);
        Mockito.verify(orderCourierMatcherMock, Mockito.times(1)).registerNotificationDeque(mockDeque);
        Assertions.assertThat(actual).isTrue();
    }

    @Test
    void mustComplete_whenCourierIsReleased() {

    }

    @Test
    void processOrderDelivered_mustCompleteExceptionally_whenCourierIdIsNotFound() {
        //given
        OrderDeliveredEvent deliveryEventMock = Mockito.mock(OrderDeliveredEvent.class);

        Mockito.doReturn(0).when(deliveryEventMock).getCourierId();
        Mockito.doThrow(NoSuchElementException.class)
                .when(fleetMock)
                .release(0);

        //when
        CompletableFuture<Void> future = underTest.processOrderDelivered(deliveryEventMock);

        //then
        ThrowableAssert.ThrowingCallable throwingCallable = () -> future.join();

        //verify
        Assertions.assertThatThrownBy(throwingCallable)
                .isInstanceOf(CompletionException.class)
                .hasMessageContaining("NoSuchElementException");
        Mockito.verify(fleetMock, Mockito.times(1)).release(0);
        Mockito.verify(deliveryEventMock, Mockito.times(1)).getCourierId();
    }

    @Test
    void processOrderDelivered_mustComplete_whenCourierIdValid() throws ExecutionException, InterruptedException {
        //given
        OrderDeliveredEvent deliveryEventMock = Mockito.mock(OrderDeliveredEvent.class);

        Mockito.doReturn(0).when(deliveryEventMock).getCourierId();
        Mockito.doNothing()
                .when(fleetMock)
                .release(0);

        //when
        CompletableFuture<Void> future = underTest.processOrderDelivered(deliveryEventMock);

        //then
        future.get();

        //verify
        Assertions.assertThat(future.isDone()).isTrue();
        Mockito.verify(fleetMock, Mockito.times(1)).release(0);
        Mockito.verify(deliveryEventMock, Mockito.times(1)).getCourierId();
    }
}