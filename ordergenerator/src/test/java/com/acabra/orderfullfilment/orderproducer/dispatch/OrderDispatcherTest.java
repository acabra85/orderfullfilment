package com.acabra.orderfullfilment.orderproducer.dispatch;

import com.acabra.orderfullfilment.orderproducer.TestUtils;
import com.acabra.orderfullfilment.orderproducer.config.RestClientConfig;
import com.acabra.orderfullfilment.orderproducer.dto.DeliveryOrderRequest;
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
import java.util.concurrent.TimeUnit;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RestClientConfig.class})
class OrderDispatcherTest {

    protected OrderDispatcher underTest;

    @Autowired
    private RestTemplate restTemplate;

    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        underTest = new OrderDispatcher(restTemplate);
    }

    @Test
    public void givenMockIsDoneByMockRestServiceServer_whenGetIsCalled_thenReturnsSuccess() throws URISyntaxException, InterruptedException {
        //given
        List<DeliveryOrderRequest> orders = TestUtils.getOrders(5);

        mockServer.expect(ExpectedCount.times(orders.size()),
                        MockRestRequestMatchers.requestTo(new URI(OrderDispatcher.ORDERS_RESOURCE)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.OK));

        //when
        underTest.dispatch(orders);

        //then
        Assertions.assertThat(underTest.registerListener().await(5, TimeUnit.SECONDS)).isTrue();
        mockServer.verify();
        OrderDispatcher.OrderDispatcherStatus actual = underTest.totalOrders();
        Assertions.assertThat(actual.success).isEqualTo(orders.size());
        Assertions.assertThat(actual.failures).isEqualTo(0);
    }

    @Test
    public void givenMockIsDoneByMockRestServiceServer_whenGetIsCalledWithSigPill_thenReturnsSuccess() throws URISyntaxException, InterruptedException {
        //given
        int expectedSuccessCalls = 3;
        int posSigPill = 3;
        List<DeliveryOrderRequest> orders = TestUtils.getOrdersWithSigPillAtPos(5, posSigPill);

        mockServer.expect(ExpectedCount.times(expectedSuccessCalls),
                        MockRestRequestMatchers.requestTo(new URI(OrderDispatcher.ORDERS_RESOURCE)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.OK));

        //when
        underTest.dispatch(orders);


        //then
        Assertions.assertThat(underTest.registerListener().await(5, TimeUnit.SECONDS)).isTrue();
        mockServer.verify();
        OrderDispatcher.OrderDispatcherStatus actual = underTest.totalOrders();
        Assertions.assertThat(actual.success).isEqualTo(expectedSuccessCalls);
        Assertions.assertThat(actual.failures).isEqualTo(0);
    }

    @Test
    public void givenMockIsDoneByMockRestServiceServer_whenCallsFail_countsFailures() throws URISyntaxException, InterruptedException {
        //given
        List<DeliveryOrderRequest> orders = TestUtils.getOrders(5);

        String responseText = "{\"statusCode\":400, \"message\": \"message\", \"body\":null}";
        mockServer.expect(ExpectedCount.times(orders.size()),
                        MockRestRequestMatchers.requestTo(new URI(OrderDispatcher.ORDERS_RESOURCE)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.BAD_REQUEST).body(responseText));

        //when
        underTest.dispatch(orders);

        //then
        Assertions.assertThat(underTest.registerListener().await(5, TimeUnit.SECONDS)).isTrue();
        mockServer.verify();
        OrderDispatcher.OrderDispatcherStatus actual = underTest.totalOrders();
        Assertions.assertThat(actual.success).isEqualTo(0);
        Assertions.assertThat(actual.failures).isEqualTo(5);
    }
}