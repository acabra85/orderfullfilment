package com.acabra.orderfullfilment.orderserver.core.executor;

import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Supplier;
import java.util.stream.IntStream;

class NoMoreOrdersMonitorTest {

    private NoMoreOrdersMonitor underTest;

    @BeforeEach
    public void setup() {
    }

    @Test
    public void mustOnlyAttemptMaxRetries_givenSupplierAlwaysReturnsFalse() {
        //given
        int maxRetries = 5;
        Deque<OutputEvent> deque = new ArrayDeque<>();

        Supplier<Boolean> supplierMock = Mockito.mock(Supplier.class);
        Mockito.doReturn(false).when(supplierMock).get();

        underTest = new NoMoreOrdersMonitor(maxRetries, supplierMock, deque);

        //when
        IntStream.range(0, 100).forEach(i -> underTest.doWork());

        //then
        Mockito.verify(supplierMock, Mockito.times(maxRetries)).get();
    }

}