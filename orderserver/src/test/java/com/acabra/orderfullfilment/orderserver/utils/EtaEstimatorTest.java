package com.acabra.orderfullfilment.orderserver.utils;

import com.acabra.orderfullfilment.orderserver.config.CourierConfig;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = CourierConfig.class)
@TestPropertySource(value = "classpath:application.properties")
class EtaEstimatorTest {

    EtaEstimator underTest;

    @Autowired
    private CourierConfig config;

    @BeforeEach
    public void setup() {
        underTest = new EtaEstimator(config);
    }

    @Test
    void mustReturnUniformValuesWithinBounds() {
        //given
        final LongSummaryStatistics stats = new LongSummaryStatistics();
        int expectedLowerBound = 3;
        int expectedUpperBound = 15;

        //when
        IntStream.range(0, 10000).forEach(i -> stats.accept(underTest.estimateCourierTravelTime()));

        //then
        Assertions.assertThat(stats.getMin()).isEqualTo(expectedLowerBound);
        Assertions.assertThat(stats.getMax()).isEqualTo(expectedUpperBound);
    }

}