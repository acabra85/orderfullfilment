package com.acabra.orderfullfilment.orderserver.core;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;

class RetryBudgetTest {


    @Test
    public void apiTest() {
        //given
        int pollingMaxRetries = 3;
        RetryBudget underTest = RetryBudget.of(pollingMaxRetries);
        ThrowableAssert.ThrowingCallable throwingCallable = underTest::spendRetryToken;

        Assertions.assertThat(underTest.hasMoreTokens()).isTrue();

        underTest.spendRetryToken();
        underTest.spendRetryToken();
        Assertions.assertThat(underTest.hasMoreTokens()).isTrue();

        underTest.spendRetryToken();
        Assertions.assertThat(underTest.hasMoreTokens()).isFalse();

        underTest.success();
        Assertions.assertThat(underTest.hasMoreTokens()).isTrue();
        underTest.success();
        Assertions.assertThat(underTest.hasMoreTokens()).isTrue();


        //when
        Assertions.assertThat(underTest.remainingTokens()).isEqualTo(pollingMaxRetries);
        underTest.spendRetryToken();
        underTest.spendRetryToken();
        underTest.spendRetryToken();

        //that
        Assertions.assertThatThrownBy(throwingCallable)
                .isExactlyInstanceOf(IllegalStateException.class)
                .hasMessageContaining("All retry tokens used");
        Assertions.assertThat(underTest.hasMoreTokens()).isFalse();

    }


    @Test
    public void shouldThrowException_givenZeroRetries() {
        //given
        int pollingMaxRetries = 0;

        //when
        ThrowableAssert.ThrowingCallable throwingCallable = () -> {
            RetryBudget.of(pollingMaxRetries);
        };

        //then
        Assertions.assertThatThrownBy(throwingCallable)
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("RetryBudget needs 1 or more retries to execute give");
    }
}