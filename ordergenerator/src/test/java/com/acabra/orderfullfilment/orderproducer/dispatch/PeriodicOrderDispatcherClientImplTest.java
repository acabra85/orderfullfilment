package com.acabra.orderfullfilment.orderproducer.dispatch;

import com.acabra.orderfullfilment.orderproducer.TestUtils;
import com.acabra.orderfullfilment.orderproducer.config.RestClientConfig;
import com.acabra.orderfullfilment.orderproducer.dto.DeliveryOrderRequestDTO;
import com.acabra.orderfullfilment.orderproducer.dto.OrderDispatcherStatusPOJO;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.ExecutionException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RestClientConfig.class})
class PeriodicOrderDispatcherClientImplTest {

    protected PeriodicOrderDispatcherClientImpl underTest;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        underTest = new PeriodicOrderDispatcherClientImpl(restTemplate);
    }

    @Test
    public void givenMockIsDoneByMockRestServiceServer_whenGetIsCalled_thenReturnsSuccess() throws URISyntaxException,
            InterruptedException, ExecutionException {
        //given
        List<DeliveryOrderRequestDTO> orders = TestUtils.buildOrderListOfSize(5);

        mockServer.expect(ExpectedCount.times(orders.size()),
                        MockRestRequestMatchers.requestTo(new URI(PeriodicOrderDispatcherClientImpl.ORDERS_RESOURCE)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.ACCEPTED));

        //when
        underTest.dispatchTwoOrdersPerSecond(orders);
        OrderDispatcherStatusPOJO actual = underTest.getCompletionFuture().get();

        //then
        mockServer.verify();
        Assertions.assertThat(actual.successCount).isEqualTo(orders.size());
        Assertions.assertThat(actual.failureCount).isEqualTo(0);
    }

    @Test
    public void givenMockIsDoneByMockRestServiceServer_whenGetIsCalledWithSigPill_thenReturnsSuccess() throws URISyntaxException,
            InterruptedException, ExecutionException {
        //given
        int expectedSuccessCalls = 3;
        int posSigPill = 3;
        List<DeliveryOrderRequestDTO> orders = TestUtils.getOrdersWithSigPillAtPos(5, posSigPill);

        mockServer.expect(ExpectedCount.times(expectedSuccessCalls),
                        MockRestRequestMatchers.requestTo(new URI(PeriodicOrderDispatcherClientImpl.ORDERS_RESOURCE)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.ACCEPTED));

        //when
        underTest.dispatchTwoOrdersPerSecond(orders);
        OrderDispatcherStatusPOJO actual = underTest.getCompletionFuture().get();

        //then
        mockServer.verify();
        Assertions.assertThat(actual.successCount).isEqualTo(expectedSuccessCalls);
        Assertions.assertThat(actual.failureCount).isEqualTo(0);
    }

    @Test
    public void givenDispatcherIsCompleted_whenRegisteringListenerHasNoEffects() throws InterruptedException,
            ExecutionException {
        //given
        int expectedSuccessCalls = 0;
        int posSigPill = 0;
        List<DeliveryOrderRequestDTO> orders = TestUtils.getOrdersWithSigPillAtPos(1, posSigPill);

        //when
        underTest.dispatchTwoOrdersPerSecond(orders);
        OrderDispatcherStatusPOJO actual = underTest.getCompletionFuture().get();

        //then
        Assertions.assertThat(actual.successCount).isEqualTo(expectedSuccessCalls);
        Assertions.assertThat(actual.failureCount).isEqualTo(0);
    }

    @Test
    public void givenMockIsDoneByMockRestServiceServer_whenCallsFail_countsFailures() throws URISyntaxException,
            ExecutionException, InterruptedException {
        //given
        List<DeliveryOrderRequestDTO> orders = TestUtils.buildOrderListOfSize(5);

        String responseText = "{\"message\": \"message\", \"body\":null}";
        mockServer.expect(ExpectedCount.times(orders.size()),
                        MockRestRequestMatchers.requestTo(new URI(PeriodicOrderDispatcherClientImpl.ORDERS_RESOURCE)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.BAD_REQUEST).body(responseText));

        //when
        underTest.dispatchTwoOrdersPerSecond(orders);
        OrderDispatcherStatusPOJO actual = underTest.getCompletionFuture().get();

        //then
        mockServer.verify();
        Assertions.assertThat(actual.successCount).isEqualTo(0);
        Assertions.assertThat(actual.failureCount).isEqualTo(5);
    }
}