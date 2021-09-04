package com.acabra.orderfullfilment.orderserver.courier;

import com.acabra.orderfullfilment.orderserver.event.CourierArrivedEvent;
import com.acabra.orderfullfilment.orderserver.courier.model.Courier;
import com.acabra.orderfullfilment.orderserver.courier.model.CourierStatus;
import com.acabra.orderfullfilment.orderserver.courier.model.DispatchResult;
import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import com.acabra.orderfullfilment.orderserver.utils.EtaEstimator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class CourierFleetImplTest {

    public static final Courier ANY_COURIER = Mockito.any(Courier.class);
    private CourierFleet underTest;

    private final DeliveryOrder validOrder = DeliveryOrder.of("order-it-test", "my-meal", 3131221);
    private final EtaEstimator etaEstimatorMock = Mockito.mock(EtaEstimator.class);

    @BeforeEach
    public void setup() {
        int fixedTravelTime = 3;
        Mockito.doReturn(fixedTravelTime).when(etaEstimatorMock).estimateCourierTravelTimeInSeconds(ANY_COURIER);
    }

    @Test
    void mustReturnNull_noCouriersAvailable() {
        //given
        List<Courier> list = buildCourierList(4, CourierStatus.DISPATCHED);
        underTest = new CourierFleetImpl(list, etaEstimatorMock);
        Assertions.assertThat(underTest.availableCouriers()).isEqualTo(0);
        Assertions.assertThat(underTest.fleetSize()).isEqualTo(list.size());

        //when
        Integer actual = underTest.dispatch(validOrder).courierId;

        //then
        Mockito.verify(etaEstimatorMock, Mockito.never()).estimateCourierTravelTimeInSeconds(ANY_COURIER);
        Assertions.assertThat(underTest).isNotNull();
        Assertions.assertThat(actual).isNull();
        Assertions.assertThat(underTest.fleetSize()).isEqualTo(list.size());
    }

    @Test
    void mustReturn0_couriersAvailable() {
        //given
        List<Courier> list = buildCourierList(4, CourierStatus.AVAILABLE);
        underTest = new CourierFleetImpl(list, etaEstimatorMock);
        int totalAvailableBefore = underTest.availableCouriers();
        Assertions.assertThat(totalAvailableBefore).isEqualTo(list.size());
        Assertions.assertThat(underTest.fleetSize()).isEqualTo(list.size());

        //when
        Integer actual = underTest.dispatch(validOrder).courierId;

        //then
        Mockito.verify(etaEstimatorMock, Mockito.times(1)).estimateCourierTravelTimeInSeconds(ANY_COURIER);
        Assertions.assertThat(underTest).isNotNull();
        Assertions.assertThat(actual).isEqualTo(0);
        Assertions.assertThat(underTest.availableCouriers()).isEqualTo(totalAvailableBefore - 1);
        Assertions.assertThat(underTest.fleetSize()).isEqualTo(list.size());
    }


    @Test
    void mustReturnFailAfter4Calls_noMoreCouriersAvailable() {
        //given
        int expectedSize = 4;
        List<Courier> availableCouriers = buildCourierList(expectedSize, CourierStatus.AVAILABLE);
        underTest = new CourierFleetImpl(availableCouriers, etaEstimatorMock);
        int totalAvailableBefore = underTest.availableCouriers();
        Assertions.assertThat(totalAvailableBefore).isEqualTo(availableCouriers.size());
        Assertions.assertThat(underTest.fleetSize()).isEqualTo(availableCouriers.size());

        //when
        List<Long> dispatched = IntStream.range(0, 10)
                .mapToObj(elm -> underTest.dispatch(validOrder))
                .filter(elm -> elm.courierId != null)
                .mapToLong(elm -> Long.valueOf(elm.courierId))
                .boxed()
                .collect(Collectors.toList());

        //then
        Mockito.verify(etaEstimatorMock, Mockito.times(expectedSize)).estimateCourierTravelTimeInSeconds(ANY_COURIER);
        Assertions.assertThat(dispatched.size()).isEqualTo(expectedSize);
        Assertions.assertThat(underTest.availableCouriers()).isEqualTo(0);
        Assertions.assertThat(underTest.fleetSize()).isEqualTo(availableCouriers.size());
    }

    @Test
    void mustFail_unableToFindCourierId() {
        //given
        List<Courier> list = buildCourierList(4, CourierStatus.AVAILABLE);
        underTest = new CourierFleetImpl(list, etaEstimatorMock);
        int courierIdInvalid = 55;
        Assertions.assertThat(list).noneMatch(courier -> courier.id == courierIdInvalid);

        //when

        //then
        Mockito.verify(etaEstimatorMock, Mockito.never()).estimateCourierTravelTimeInSeconds(ANY_COURIER);
        Assertions.assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> underTest.release(courierIdInvalid))
                .withMessageContaining("does not correspond to an assigned courier");
    }

    @Test
    void mustSucceed_givenCourierIdIsAssigned() {
        //given
        List<Courier> list = buildCourierList(4, CourierStatus.DISPATCHED);
        underTest = new CourierFleetImpl(list, etaEstimatorMock);
        int validAssignedId = 3;
        Assertions.assertThat(list).anyMatch(courier -> courier.id == validAssignedId);
        int totalAvailableBefore = underTest.availableCouriers();

        //when
        underTest.release(validAssignedId);

        //then
        Assertions.assertThat(underTest.availableCouriers()).isEqualTo(1 + totalAvailableBefore);

    }

    @Test
    public void mustReportArrivalNotificationAfterSuccessfulDispatch() throws InterruptedException, ExecutionException {
        //given
        List<Courier> list = buildCourierList(1, CourierStatus.AVAILABLE);
        BlockingDeque<OutputEvent> deque = new LinkedBlockingDeque<>();
        underTest = new CourierFleetImpl(list, etaEstimatorMock);
        underTest.registerNotificationDeque(deque);
        int expectedCourierId = 0;
        CompletableFuture<OutputEvent> future = CompletableFuture.supplyAsync(() -> {
            try {
                return deque.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        });

        //when
        Integer actualCourierId = underTest.dispatch(validOrder).courierId;
        CourierArrivedEvent actualEvent = (CourierArrivedEvent) future.get();

        //then
        Mockito.verify(etaEstimatorMock, Mockito.times(1)).estimateCourierTravelTimeInSeconds(ANY_COURIER);
        Assertions.assertThat(underTest.availableCouriers()).isEqualTo(0);
        Assertions.assertThat(actualCourierId).isEqualTo(expectedCourierId);

        Assertions.assertThat(actualEvent).isNotNull();
        Assertions.assertThat(actualEvent.courierId).isEqualTo(expectedCourierId);
        Assertions.assertThat(actualEvent.createdAt).isGreaterThan(actualEvent.eta);
    }

    @Test
    public void mustFailCourierNotification_exceptionThrownWhileReporting() throws InterruptedException {
        //given
        List<Courier> list = buildCourierList(1, CourierStatus.AVAILABLE);
        BlockingDeque<OutputEvent> dequeMock = Mockito.mock(LinkedBlockingDeque.class);
        Mockito.doThrow(RuntimeException.class).when(dequeMock).offer(Mockito.any(CourierArrivedEvent.class));

        underTest = new CourierFleetImpl(list, etaEstimatorMock);
        underTest.registerNotificationDeque(dequeMock);

        int expectedCourierId = 0;

        //when
        DispatchResult actualDispatchResult = underTest.dispatch(validOrder);
        Integer actualCourierId = actualDispatchResult.courierId;

        //then
        Assertions.assertThat(actualCourierId).isEqualTo(expectedCourierId);
        Assertions.assertThatThrownBy(actualDispatchResult.notificationFuture::get)
                .isInstanceOf(ExecutionException.class)
                .hasMessageContaining("RuntimeException");
        Mockito.verify(etaEstimatorMock, Mockito.times(1)).estimateCourierTravelTimeInSeconds(ANY_COURIER);
        Mockito.verify(dequeMock, Mockito.times(1)).offer(Mockito.any(CourierArrivedEvent.class));
        Assertions.assertThat(underTest.availableCouriers()).isEqualTo(0);
    }

    @Test
    public void mustCompleteNotificationAsFalse_interruptedExceptionThrown() throws InterruptedException, ExecutionException {
        //given
        List<Courier> list = buildCourierList(1, CourierStatus.AVAILABLE);
        BlockingDeque<OutputEvent> dequeMock = Mockito.mock(LinkedBlockingDeque.class);
        Mockito.doThrow(InterruptedException.class).when(dequeMock).offer(Mockito.any(CourierArrivedEvent.class));

        underTest = new CourierFleetImpl(list, etaEstimatorMock);
        underTest.registerNotificationDeque(dequeMock);

        int expectedCourierId = 0;

        //when
        DispatchResult actualDispatchResult = underTest.dispatch(validOrder);
        Integer actualCourierId = actualDispatchResult.courierId;

        //then
        Assertions.assertThat(actualCourierId).isEqualTo(expectedCourierId);
        Assertions.assertThat(actualDispatchResult.notificationFuture.get()).isFalse();
        Mockito.verify(etaEstimatorMock, Mockito.times(1)).estimateCourierTravelTimeInSeconds(ANY_COURIER);
        Mockito.verify(dequeMock, Mockito.times(1)).offer(Mockito.any(CourierArrivedEvent.class));
        Assertions.assertThat(underTest.availableCouriers()).isEqualTo(0);
    }

    @Test
    public void mustCompleteNotificationAsFalse_noDequeAvailable() throws InterruptedException, ExecutionException {
        //given
        List<Courier> list = buildCourierList(1, CourierStatus.AVAILABLE);
        underTest = new CourierFleetImpl(list, etaEstimatorMock);

        int expectedCourierId = 0;

        //when
        DispatchResult actualDispatchResult = underTest.dispatch(validOrder);
        Integer actualCourierId = actualDispatchResult.courierId;

        //then
        Assertions.assertThat(actualCourierId).isEqualTo(expectedCourierId);
        Assertions.assertThat(actualDispatchResult.notificationFuture.get()).isFalse();
        Assertions.assertThat(underTest.availableCouriers()).isEqualTo(0);
    }

    @Test
    public void mustFail() {
    }

    private List<Courier> buildCourierList(int size, CourierStatus status) {
        return IntStream.range(0, size)
                .mapToObj(i -> CourierStatus.AVAILABLE == status ?
                        Courier.ofAvailable(i, "Courier" + i) :
                        Courier.ofDispatched(i, "Courier" + i))
                .collect(Collectors.toList());
    }

}