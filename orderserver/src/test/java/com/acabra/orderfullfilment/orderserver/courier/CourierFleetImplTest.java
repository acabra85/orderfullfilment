package com.acabra.orderfullfilment.orderserver.courier;

import com.acabra.orderfullfilment.orderserver.courier.model.Courier;
import com.acabra.orderfullfilment.orderserver.courier.model.CourierStatus;
import com.acabra.orderfullfilment.orderserver.model.DeliveryOrder;
import com.acabra.orderfullfilment.orderserver.utils.EtaEstimator;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class CourierFleetImplTest {

    private CourierFleet underTest;

    private final DeliveryOrder validOrder = new DeliveryOrder("order-it-test", 2131231, 3131221);
    private EtaEstimator etaEstimatorMock = Mockito.mock(EtaEstimator.class);

    @BeforeEach
    public void setup() {
        Mockito.when(etaEstimatorMock.estimateCourierTravelTime()).thenReturn(3);
    }

    @Test
    void mustReturnNull_noCouriersAvailable() {
        //given
        List<Courier> list = buildCourierList(4, CourierStatus.DISPATCHED);
        underTest = new CourierFleetImpl(list, etaEstimatorMock);
        Assertions.assertThat(underTest.availableCouriers()).isEqualTo(0);
        Assertions.assertThat(underTest.fleetSize()).isEqualTo(list.size());

        //when
        Integer actual = underTest.dispatch(validOrder);

        //then
        Mockito.verify(etaEstimatorMock, Mockito.never()).estimateCourierTravelTime();
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
        Integer actual = underTest.dispatch(validOrder);

        //then
        Mockito.verify(etaEstimatorMock, Mockito.times(1)).estimateCourierTravelTime();
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
                .mapToObj(i -> underTest.dispatch(validOrder))
                .filter(i -> i != null)
                .mapToLong(i -> Long.valueOf(i))
                .boxed()
                .collect(Collectors.toList());

        //then
        Mockito.verify(etaEstimatorMock, Mockito.times(expectedSize)).estimateCourierTravelTime();
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
        Mockito.verify(etaEstimatorMock, Mockito.never()).estimateCourierTravelTime();
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

    private List<Courier> buildCourierList(int size, CourierStatus status) {
        return IntStream.range(0, size)
                .mapToObj(i -> CourierStatus.AVAILABLE == status ?
                        Courier.ofAvailable(i, "Courier" + i) :
                        Courier.ofDispatched(i, "Courier" + i))
                .collect(Collectors.toList());
    }

}