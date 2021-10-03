package com.acabra.orderfullfilment.orderserver.courier;

import com.acabra.orderfullfilment.orderserver.courier.matcher.OrderCourierMatcher;
import com.acabra.orderfullfilment.orderserver.courier.matcher.OrderCourierMatcherFIFOImpl;
import com.acabra.orderfullfilment.orderserver.courier.model.DispatchResult;
import com.acabra.orderfullfilment.orderserver.event.*;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Deque;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;

class CourierServiceImplTest {

    private final OrderPreparedEvent VALID_ORDER_PREPARED_EVENT = OrderPreparedEvent.of(3213, "some-id", 3123123);
    private final long NOW = 1000000000L;

    private CourierServiceImpl underTest;
    private CourierFleet fleetMock;
    private OrderCourierMatcher orderCourierMatcherMock;
    private Deque<OutputEvent> mockDeque;

    @BeforeEach
    @SuppressWarnings("unchecked")
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
        long reservationId = 0L;
        Mockito.when(fleetMock.dispatch(null, NOW)).thenReturn(DispatchResult.notDispatched());

        //when
        Optional<Integer> actual = underTest.dispatchRequest(null, reservationId, NOW);

        //then
        Mockito.verify(fleetMock).dispatch(null, NOW);
        Assertions.assertThat(actual.isEmpty()).isTrue();
    }

    @Test
    void mustReturnCourierIdAvailableCourier() {
        //given
        long reservationId = 0L;
        Mockito.when(fleetMock.dispatch(null, NOW)).thenReturn(DispatchResult.ofCompleted(5, 1));

        //when
        Optional<Integer> actual = underTest.dispatchRequest(null, reservationId, NOW);

        //then
        Mockito.verify(fleetMock).dispatch(null, NOW);
        Assertions.assertThat(actual.isPresent()).isTrue();
        Assertions.assertThat(actual.get()).isEqualTo(5);
    }

    @Test
    void mustComplete_whenCouriersAvailableForPickup() {
        //given
        underTest = new CourierServiceImpl(fleetMock, orderCourierMatcherMock);
        Mockito.doReturn(true)
                .when(orderCourierMatcherMock)
                .acceptOrderPreparedEvent(VALID_ORDER_PREPARED_EVENT);
        Mockito.doNothing()
                .when(orderCourierMatcherMock)
                .registerNotificationDeque(mockDeque);
        Mockito.doNothing()
                .when(fleetMock)
                .registerNotificationDeque(mockDeque);
        underTest.registerNotificationDeque(mockDeque);

        //when
        boolean actual = underTest.processOrderPrepared(VALID_ORDER_PREPARED_EVENT);

        //then
        Mockito.verifyNoInteractions(mockDeque);
        Mockito.verify(fleetMock, Mockito.times(1)).registerNotificationDeque(mockDeque);
        Mockito.verify(orderCourierMatcherMock, Mockito.times(1)).acceptOrderPreparedEvent(VALID_ORDER_PREPARED_EVENT);
        Mockito.verify(orderCourierMatcherMock, Mockito.times(1)).registerNotificationDeque(mockDeque);
        Assertions.assertThat(actual).isTrue();
    }

    @Test
    void processOrderDelivered_mustComplete_whenCourierIdValid() {
        //given
        OrderDeliveredEvent deliveryEventMock = Mockito.mock(OrderDeliveredEvent.class);

        Mockito.doReturn(0).when(deliveryEventMock).getCourierId();
        Mockito.doNothing()
                .when(fleetMock)
                .release(0);

        //when
        underTest.processOrderDelivered(deliveryEventMock);

        //then

        //verify
        Mockito.verify(fleetMock, Mockito.times(1)).release(0);
        Mockito.verify(deliveryEventMock, Mockito.times(1)).getCourierId();
    }

    @Test
    void processCourierArrived_mustComplete_whenCourierIdValid() {
        //given
        CourierArrivedEvent courierArrivedMock = Mockito.mock(CourierArrivedEvent.class);

        Mockito.doNothing()
                .when(fleetMock)
                .release(0);
        Mockito.doReturn(true).when(orderCourierMatcherMock).acceptCourierArrivedEvent(courierArrivedMock);

        //when
        boolean actual = underTest.processCourierArrived(courierArrivedMock);

        //then
        //verify
        Assertions.assertThat(actual).isEqualTo(true);
        Mockito.verify(fleetMock, Mockito.times(0)).release(0);
        Mockito.verify(orderCourierMatcherMock, Mockito.times(1)).acceptCourierArrivedEvent(courierArrivedMock);
    }

    @Test
    public void mustCallTheCourierMatcher_givenADispachedEventIsReceived() {
        //given
        CourierDispatchedEvent courierDispatchedEvtMock = Mockito.mock(CourierDispatchedEvent.class);
        Mockito.doNothing().when(orderCourierMatcherMock).processCourierDispatchedEvent(courierDispatchedEvtMock);

        //when
        underTest.processCourierDispatchedEvent(courierDispatchedEvtMock);

        //then
        //verify
        Mockito.verify(orderCourierMatcherMock, Mockito.times(1)).processCourierDispatchedEvent(courierDispatchedEvtMock);
    }

    @Test
    public void mustLogError_givenFailedToPublishEvent() {
        //given
        DispatchResult dispatchResultStub = DispatchResult.of(2500, CompletableFuture.completedFuture(true), 1000L);
        DeliveryOrder orderStub = DeliveryOrder.of("order-id", "some-food", 3);
        Mockito.doNothing()
                .when(orderCourierMatcherMock)
                .registerNotificationDeque(mockDeque);
        Mockito.doReturn(dispatchResultStub)
                .when(fleetMock)
                .dispatch(orderStub, NOW);
        Mockito.doNothing()
                .when(fleetMock)
                .registerNotificationDeque(mockDeque);
        Mockito.doThrow(new RuntimeException("unable to offer"))
                .when(mockDeque)
                .offer(Mockito.any(CourierDispatchedEvent.class));

        underTest = new CourierServiceImpl(fleetMock, orderCourierMatcherMock);
        underTest.registerNotificationDeque(mockDeque);

        //when
        Optional<Integer> actual = underTest.dispatchRequest(orderStub, 1, NOW);

        //then
        Mockito.verify(fleetMock, Mockito.times(1)).registerNotificationDeque(mockDeque);
        Mockito.verify(fleetMock, Mockito.times(1)).dispatch(orderStub, NOW);
        Mockito.verify(mockDeque, Mockito.times(1)).offer(Mockito.any(CourierDispatchedEvent.class));
        Mockito.verify(orderCourierMatcherMock, Mockito.times(1)).registerNotificationDeque(mockDeque);
        Assertions.assertThat(actual.orElse(null)).isEqualTo(2500);
    }
}