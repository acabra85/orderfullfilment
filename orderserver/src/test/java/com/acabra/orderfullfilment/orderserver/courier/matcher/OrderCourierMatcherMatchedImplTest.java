package com.acabra.orderfullfilment.orderserver.courier.matcher;

import com.acabra.orderfullfilment.orderserver.TestUtils.DispatchMatch;

import com.acabra.orderfullfilment.orderserver.TestUtils;
import com.acabra.orderfullfilment.orderserver.event.CourierArrivedEvent;
import com.acabra.orderfullfilment.orderserver.event.OrderPreparedEvent;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

class OrderCourierMatcherMatchedImplTest {

    private OrderCourierMatcherMatchedImpl underTest;

    @BeforeEach
    public void setup() {
        underTest = new OrderCourierMatcherMatchedImpl();
    }

    @Test
    public void shouldNotMatchOrder_givenMatchCourierNotPresent() {
        //given
        OrderPreparedEvent orderWillAwait = OrderPreparedEvent.of(10, "", 1000);

        //register events to matcher
        buildMatchedOrdersAndCouriers().stream()
                .map(TestUtils::buildDispatchEvent)
                .forEach(underTest::processCourierDispatchedEvent);

        //when
        boolean actual = underTest.acceptOrderPreparedEvent(orderWillAwait);

        //then
        Assertions.assertThat(actual).isTrue();
    }

    @Test
    public void shouldMatchOrder_givenMatchCourierIsWaiting() {
        //given
        OrderPreparedEvent orderWillBeMatched = OrderPreparedEvent.of(10, "", 1000);
        CourierArrivedEvent courierWaiting = CourierArrivedEvent.of(1, 100, 100);

        //register events to matcher
        buildMatchedOrdersAndCouriers().stream()
                .map(TestUtils::buildDispatchEvent)
                .forEach(underTest::processCourierDispatchedEvent);

        //place courier to wait
        underTest.acceptCourierArrivedEvent(courierWaiting);

        //when
        boolean actual = underTest.acceptOrderPreparedEvent(orderWillBeMatched);

        //then
        Assertions.assertThat(actual).isTrue();
    }

    @Test
    public void shouldFail_givenExceptionThrownUnrecognizedOrder() {
        //given
        OrderPreparedEvent unrecognizedOrder = OrderPreparedEvent.of(10, "", 1000);

        //when
        boolean actual = underTest.acceptOrderPreparedEvent(unrecognizedOrder);

        //then
        Assertions.assertThat(actual).isFalse();
    }

    @Test
    public void shouldNotMatchCourier_givenMatchOrderNotPresent() {
        //given
        CourierArrivedEvent courierWillWait = CourierArrivedEvent.of(1, 100, 100);

        //register events to matcher
        buildMatchedOrdersAndCouriers().stream()
                .map(TestUtils::buildDispatchEvent)
                .forEach(underTest::processCourierDispatchedEvent);

        //when
        boolean actual = underTest.acceptCourierArrivedEvent(courierWillWait);

        //then
        Assertions.assertThat(actual).isTrue();
    }

    @Test
    public void shouldMatchCourier_givenMatchOrderIsWaiting() {
        //given
        CourierArrivedEvent courierWillBeMatched = CourierArrivedEvent.of(1, 100, 100);
        OrderPreparedEvent orderWaiting = OrderPreparedEvent.of(10, "", 0);

        //register events to matcher
        buildMatchedOrdersAndCouriers().stream()
                .map(TestUtils::buildDispatchEvent)
                .forEach(underTest::processCourierDispatchedEvent);

        //place courier to wait
        underTest.acceptOrderPreparedEvent(orderWaiting);

        //when
        boolean actual = underTest.acceptCourierArrivedEvent(courierWillBeMatched);

        //then
        Assertions.assertThat(actual).isTrue();
    }

    @Test
    public void shouldFail_givenExceptionThrownUnrecognizedCourier() {
        //given
        CourierArrivedEvent unrecognizedCourier = CourierArrivedEvent.of(10, 0, 0);

        //when
        boolean actual = underTest.acceptCourierArrivedEvent(unrecognizedCourier);

        //then
        Assertions.assertThat(actual).isFalse();
    }


    @Test
    void registerNotificationDeque() {
    }

    private List<DispatchMatch> buildMatchedOrdersAndCouriers() {
        return List.of(DispatchMatch.builder().withCourier(1).withOrder(10).build(),
                DispatchMatch.builder().withCourier(2).withOrder(20).build(),
                DispatchMatch.builder().withCourier(3).withOrder(30).build());
    }
}