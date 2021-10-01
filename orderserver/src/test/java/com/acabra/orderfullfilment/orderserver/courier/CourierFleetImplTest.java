package com.acabra.orderfullfilment.orderserver.courier;

import com.acabra.orderfullfilment.orderserver.config.OrderServerConfig;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Deque;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {OrderServerConfig.class})
@TestPropertySource(value = "classpath:application.properties")
class CourierFleetImplTest {

    private CourierFleet underTest;
    private final long NOW = 10000000L;

    private final DeliveryOrder validOrder = DeliveryOrder.of("order-it-test", "my-meal", 3131221);
    private final EtaEstimator etaEstimatorMock = Mockito.mock(EtaEstimator.class);

    @BeforeEach
    public void setup() {
        int fixedTravelTime = 3;
        Mockito.doReturn(fixedTravelTime).when(etaEstimatorMock).estimateCourierTravelTimeInSeconds(Mockito.any(Courier.class));
    }

    @Test
    void mustDispatchANewCourier_givenAllExistingCouriersAreDispatched() {
        //given
        List<Courier> originalCouriers = buildCourierList(4, CourierStatus.DISPATCHED);
        underTest = new CourierFleetImpl(originalCouriers, etaEstimatorMock);
        Assertions.assertThat(underTest.availableCouriers()).isEqualTo(0);
        Assertions.assertThat(underTest.fleetSize()).isEqualTo(originalCouriers.size());

        //when
        Integer actual = underTest.dispatch(validOrder, NOW).courierId;

        //then
        Mockito.verify(etaEstimatorMock, Mockito.times(1)).estimateCourierTravelTimeInSeconds(Mockito.any(Courier.class));
        Assertions.assertThat(actual).isEqualTo(5);
        Assertions.assertThat(underTest.fleetSize()).isEqualTo(originalCouriers.size() + 1);
        Assertions.assertThat(originalCouriers.stream().noneMatch(Courier::isAvailable)).isTrue();
    }

    @Test
    void mustReturn0_couriersAvailable() {
        //given
        List<Courier> originalCouriers = buildCourierList(4, CourierStatus.AVAILABLE);
        underTest = new CourierFleetImpl(originalCouriers, etaEstimatorMock);
        int totalAvailableBefore = underTest.availableCouriers();
        Assertions.assertThat(totalAvailableBefore).isEqualTo(originalCouriers.size());
        Assertions.assertThat(underTest.fleetSize()).isEqualTo(originalCouriers.size());

        //when
        Integer actual = underTest.dispatch(validOrder, NOW).courierId;

        //then
        Mockito.verify(etaEstimatorMock, Mockito.times(1)).estimateCourierTravelTimeInSeconds(Mockito.any(Courier.class));
        Assertions.assertThat(originalCouriers.stream().filter(c->!c.isAvailable()).count()).isEqualTo(1);
        Assertions.assertThat(actual).isEqualTo(0);
        Assertions.assertThat(underTest.availableCouriers()).isEqualTo(totalAvailableBefore - 1);
        Assertions.assertThat(underTest.fleetSize()).isEqualTo(originalCouriers.size());
    }


    @Test
    void mustDispatchAllCouriers_unlimitedCouriersAvailable() {
        //given
        int startingCouriers = 4;
        List<Courier> originalCouriers = buildCourierList(startingCouriers, CourierStatus.AVAILABLE);
        underTest = new CourierFleetImpl(originalCouriers, etaEstimatorMock);
        int totalAvailableBefore = underTest.availableCouriers();
        Assertions.assertThat(totalAvailableBefore).isEqualTo(originalCouriers.size());
        Assertions.assertThat(underTest.fleetSize()).isEqualTo(originalCouriers.size());

        //when
        int ordersRequests = 10;
        List<Long> dispatched = IntStream.range(0, ordersRequests)
                .mapToObj(elm -> underTest.dispatch(validOrder, NOW))
                .filter(elm -> elm.courierId != null)
                .mapToLong(elm -> Long.valueOf(elm.courierId))
                .boxed()
                .collect(Collectors.toList());

        //then
        Mockito.verify(etaEstimatorMock, Mockito.times(ordersRequests)).estimateCourierTravelTimeInSeconds(Mockito.any(Courier.class));
        Assertions.assertThat(originalCouriers.stream().noneMatch(Courier::isAvailable)).isTrue();
        Assertions.assertThat(dispatched.size()).isEqualTo(ordersRequests);
        Assertions.assertThat(underTest.availableCouriers()).isEqualTo(0);
        Assertions.assertThat(underTest.fleetSize()).isEqualTo(originalCouriers.size() + (ordersRequests - startingCouriers));
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
        Mockito.verify(etaEstimatorMock, Mockito.never()).estimateCourierTravelTimeInSeconds(Mockito.any(Courier.class));
        Assertions.assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> underTest.release(courierIdInvalid))
                .withMessageContaining("does not correspond to an assigned courier");
    }

    @Test
    void mustSucceedReleaseOneCourier_givenAllCouriersDispatched() {
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
        Deque<OutputEvent> deque = new ConcurrentLinkedDeque<>();
        underTest = new CourierFleetImpl(list, etaEstimatorMock);
        underTest.registerNotificationDeque(deque);
        int expectedCourierId = 0;

        //when
        DispatchResult dispatch = underTest.dispatch(validOrder, NOW);

        Integer actualCourierId = dispatch.courierId;
        Assertions.assertThat(dispatch.notificationFuture.get()).isTrue();
        CourierArrivedEvent actualEvent = (CourierArrivedEvent) deque.poll();

        //then
        Assertions.assertThat(list.stream().noneMatch(Courier::isAvailable)).isTrue();
        Mockito.verify(etaEstimatorMock, Mockito.times(1)).estimateCourierTravelTimeInSeconds(Mockito.any(Courier.class));
        Assertions.assertThat(underTest.availableCouriers()).isEqualTo(0);
        Assertions.assertThat(actualCourierId).isEqualTo(expectedCourierId);

        Assertions.assertThat(actualEvent).isNotNull();
        Assertions.assertThat(actualEvent.courierId).isEqualTo(expectedCourierId);
        Assertions.assertThat(actualEvent.createdAt).isGreaterThan(actualEvent.ett);
    }

    @Test
    public void mustFailCourierNotification_exceptionThrownWhileReporting() {
        //given
        List<Courier> list = buildCourierList(1, CourierStatus.AVAILABLE);
        Deque<OutputEvent> dequeMock = Mockito.mock(ConcurrentLinkedDeque.class);
        Mockito.doThrow(RuntimeException.class).when(dequeMock).offer(Mockito.any(CourierArrivedEvent.class));

        underTest = new CourierFleetImpl(list, etaEstimatorMock);
        underTest.registerNotificationDeque(dequeMock);

        int expectedCourierId = 0;

        //when
        DispatchResult actualDispatchResult = underTest.dispatch(validOrder, NOW);
        Boolean isNotificationSent = actualDispatchResult.notificationFuture.join();

        //then
        Assertions.assertThat(list.stream().noneMatch(Courier::isAvailable)).isTrue();
        Assertions.assertThat(actualDispatchResult.courierId).isEqualTo(expectedCourierId);
        Assertions.assertThat(isNotificationSent).isFalse();
        Mockito.verify(etaEstimatorMock, Mockito.times(1)).estimateCourierTravelTimeInSeconds(Mockito.any(Courier.class));
        Mockito.verify(dequeMock, Mockito.times(1)).offer(Mockito.any(CourierArrivedEvent.class));
        Assertions.assertThat(underTest.availableCouriers()).isEqualTo(0);
    }

    @Test
    public void mustCompleteNotificationAsFalse_interruptedExceptionThrown() {
        //given
        List<Courier> list = buildCourierList(1, CourierStatus.AVAILABLE);
        Deque<OutputEvent> dequeMock = Mockito.mock(ConcurrentLinkedDeque.class);
        Mockito.doThrow(RuntimeException.class).when(dequeMock).offer(Mockito.any(CourierArrivedEvent.class));

        underTest = new CourierFleetImpl(list, etaEstimatorMock);
        underTest.registerNotificationDeque(dequeMock);

        int expectedCourierId = 0;

        //when
        DispatchResult actualDispatchResult = underTest.dispatch(validOrder, NOW);
        Boolean isNotificationSent = actualDispatchResult.notificationFuture.join();

        //then
        Assertions.assertThat(list.stream().noneMatch(Courier::isAvailable)).isTrue();
        Assertions.assertThat(actualDispatchResult.courierId).isEqualTo(expectedCourierId);
        Assertions.assertThat(isNotificationSent).isFalse();
        Mockito.verify(etaEstimatorMock, Mockito.times(1)).estimateCourierTravelTimeInSeconds(Mockito.any(Courier.class));
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
        DispatchResult actualDispatchResult = underTest.dispatch(validOrder, NOW);

        //then
        Assertions.assertThat(list.stream().noneMatch(Courier::isAvailable)).isTrue();
        Assertions.assertThat(actualDispatchResult.courierId).isEqualTo(expectedCourierId);
        Assertions.assertThat(actualDispatchResult.notificationFuture.get()).isFalse();
        Assertions.assertThat(underTest.availableCouriers()).isEqualTo(0);
    }

    private List<Courier> buildCourierList(int size, CourierStatus status) {
        return IntStream.range(0, size)
                .mapToObj(i -> CourierStatus.AVAILABLE == status ?
                        Courier.ofAvailable(i, "Courier" + i) :
                        Courier.ofDispatched(i, "Courier" + i))
                .collect(Collectors.toList());
    }

}