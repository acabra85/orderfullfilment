package com.acabra.orderfullfilment.couriermodule.model;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

class CourierTest {

    @Test
    public void shouldCreateCourierMatched() {
        int id = 1;
        String name = "name";
        Courier underTest = Courier.ofDispatched(id, name);

        Assertions.assertThat(underTest).isNotNull();
        Assertions.assertThat(underTest.name).isEqualTo(name);
        Assertions.assertThat(underTest.id).isEqualTo(id);
        Assertions.assertThat(underTest.getStatus()).isEqualTo(CourierStatus.DISPATCHED);
        Assertions.assertThat(underTest.isAvailable()).isFalse();
    }

    @Test
    public void shouldCreateCourierAvailable() {
        int id = 1;
        String name = "name";
        Courier underTest = Courier.ofAvailable(id, name);

        Assertions.assertThat(underTest).isNotNull();
        Assertions.assertThat(underTest.name).isEqualTo(name);
        Assertions.assertThat(underTest.id).isEqualTo(id);
        Assertions.assertThat(underTest.getStatus()).isEqualTo(CourierStatus.AVAILABLE);
        Assertions.assertThat(underTest.isAvailable()).isTrue();
    }

    @Test
    public void shouldBeDispatched() {
        //given
        int id = 1;
        String name = "name";
        Courier underTest = Courier.ofAvailable(id, name);

        Assertions.assertThat(underTest).isNotNull();
        Assertions.assertThat(underTest.name).isEqualTo(name);
        Assertions.assertThat(underTest.id).isEqualTo(id);
        Assertions.assertThat(underTest.getStatus()).isEqualTo(CourierStatus.AVAILABLE);
        Assertions.assertThat(underTest.isAvailable()).isTrue();

        //when
        underTest.dispatch();

        //then
        Assertions.assertThat(underTest.getStatus()).isEqualTo(CourierStatus.DISPATCHED);
        Assertions.assertThat(underTest.isAvailable()).isFalse();

    }

    @Test
    public void shouldBeAvailableAfterOrderDelivery() {
        int id = 1;
        String name = "name";
        Courier underTest = Courier.ofDispatched(id, name);

        Assertions.assertThat(underTest).isNotNull();
        Assertions.assertThat(underTest.name).isEqualTo(name);
        Assertions.assertThat(underTest.id).isEqualTo(id);
        Assertions.assertThat(underTest.getStatus()).isEqualTo(CourierStatus.DISPATCHED);
        Assertions.assertThat(underTest.isAvailable()).isFalse();

        //when
        underTest.orderDelivered();

        //then
        Assertions.assertThat(underTest.getStatus()).isEqualTo(CourierStatus.AVAILABLE);
        Assertions.assertThat(underTest.isAvailable()).isTrue();
    }

    @Test
    public void shouldFailCourierAlreadyAvailable() {
        int id = 1;
        String name = "name";
        Courier underTest = Courier.ofAvailable(id, name);

        Assertions.assertThat(underTest).isNotNull();
        Assertions.assertThat(underTest.name).isEqualTo(name);
        Assertions.assertThat(underTest.id).isEqualTo(id);
        Assertions.assertThat(underTest.getStatus()).isEqualTo(CourierStatus.AVAILABLE);
        Assertions.assertThat(underTest.isAvailable()).isTrue();

        //when
        Assertions.assertThatThrownBy(underTest::orderDelivered)
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already available");
    }

    @Test
    public void ensureArrivalTimeWithinBounds() {
        int lowerBound = 3;
        int higherBound = 15;
        boolean allWithinBounds = IntStream.range(0, 10000)
                .map(i -> Courier.calculateArrivalTime())
                .allMatch(arrivalTime -> arrivalTime >= lowerBound && arrivalTime <= higherBound);
        Assertions.assertThat(allWithinBounds).isTrue();
    }
}