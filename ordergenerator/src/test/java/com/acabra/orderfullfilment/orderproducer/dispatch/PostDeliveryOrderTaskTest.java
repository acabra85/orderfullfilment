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
import java.util.Iterator;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RestClientConfig.class})
class PostDeliveryOrderTaskTest {

    private PostDeliveryOrderTask underTest;

    @Autowired
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;

    @BeforeEach
    public void setup() {
        this.mockServer = MockRestServiceServer.createServer(this.restTemplate);
    }

    @Test
    public void shouldFail_errorWithServer() throws URISyntaxException {
        //given
        OrderDispatcher dispatcher = new OrderDispatcher(this.restTemplate);
        OrderDispatcher.OrderDispatcherStatus beforeExecution = dispatcher.totalOrders();

        Iterator<DeliveryOrderRequest> iterator = TestUtils.getOrders(3).iterator();
        this.underTest = new PostDeliveryOrderTask(dispatcher, iterator);
        mockServer.expect(ExpectedCount.times(1),
                        MockRestRequestMatchers.requestTo(new URI(OrderDispatcher.ORDERS_RESOURCE)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond((r) -> {
                    throw new RuntimeException("error");
                });
        //when
        underTest.run();

        //verify
        mockServer.verify();

        //then
        OrderDispatcher.OrderDispatcherStatus actual = dispatcher.totalOrders();
        Assertions.assertThat(actual.failures).isEqualTo(beforeExecution.failures + 1L);
        Assertions.assertThat(actual.success).isEqualTo(beforeExecution.success);
    }

    @Test
    public void shouldNotCompleteAnyOrderEmptyIterator() throws URISyntaxException {
        //given
        OrderDispatcher dispatcher = new OrderDispatcher(this.restTemplate);
        OrderDispatcher.OrderDispatcherStatus beforeExecution = dispatcher.totalOrders();

        Iterator<DeliveryOrderRequest> iterator = TestUtils.getOrders(0).iterator();
        this.underTest = new PostDeliveryOrderTask(dispatcher, iterator);
        mockServer.expect(ExpectedCount.never(),
                        MockRestRequestMatchers.requestTo(new URI(OrderDispatcher.ORDERS_RESOURCE)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond((r) -> {
                    throw new RuntimeException("error");
                });
        //when
        underTest.run();

        //verify
        mockServer.verify();

        //then
        OrderDispatcher.OrderDispatcherStatus actual = dispatcher.totalOrders();
        Assertions.assertThat(actual.failures).isEqualTo(beforeExecution.failures);
        Assertions.assertThat(actual.success).isEqualTo(beforeExecution.success);
    }

    @Test
    public void shouldSucceedExecutionOfTwoOrders_noSigPill() throws URISyntaxException {
        //given
        OrderDispatcher dispatcher = new OrderDispatcher(this.restTemplate);
        OrderDispatcher.OrderDispatcherStatus beforeExecution = dispatcher.totalOrders();

        Iterator<DeliveryOrderRequest> iterator = TestUtils.getOrders(10).iterator();
        int expectedMockCalls = PostDeliveryOrderTask.TOTAL_ORDERS_PER_SECOND;
        this.underTest = new PostDeliveryOrderTask(dispatcher, iterator);
        mockServer.expect(ExpectedCount.times(expectedMockCalls),
                        MockRestRequestMatchers.requestTo(new URI(OrderDispatcher.ORDERS_RESOURCE)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.OK));
        //when
        underTest.run();

        //verify
        mockServer.verify();

        //then
        OrderDispatcher.OrderDispatcherStatus actual = dispatcher.totalOrders();
        Assertions.assertThat(actual.failures).isEqualTo(beforeExecution.failures);
        Assertions.assertThat(actual.success).isEqualTo(beforeExecution.success + expectedMockCalls);
    }

    @Test
    public void shouldSucceed1Out2Orders_sigPill() throws URISyntaxException {
        //given
        OrderDispatcher dispatcher = new OrderDispatcher(this.restTemplate);
        OrderDispatcher.OrderDispatcherStatus beforeExecution = dispatcher.totalOrders();

        Iterator<DeliveryOrderRequest> iterator = TestUtils.getOrdersWithSigPillAtPos(2, 1).iterator();
        this.underTest = new PostDeliveryOrderTask(dispatcher, iterator);

        int expectedMockCalls = 1;
        mockServer.expect(ExpectedCount.times(expectedMockCalls),
                        MockRestRequestMatchers.requestTo(new URI(OrderDispatcher.ORDERS_RESOURCE)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.OK));
        //when
        underTest.run();

        //verify
        mockServer.verify();

        //then
        OrderDispatcher.OrderDispatcherStatus actual = dispatcher.totalOrders();
        Assertions.assertThat(actual.failures).isEqualTo(beforeExecution.failures);
        Assertions.assertThat(actual.success).isEqualTo(beforeExecution.success + expectedMockCalls);
    }

    @Test
    public void shouldSucceed0Out2Orders_sigPill() throws URISyntaxException {
        //given
        OrderDispatcher dispatcher = new OrderDispatcher(this.restTemplate);
        OrderDispatcher.OrderDispatcherStatus beforeExecution = dispatcher.totalOrders();

        Iterator<DeliveryOrderRequest> iterator = TestUtils.getOrdersWithSigPillAtPos(2, 0).iterator();
        this.underTest = new PostDeliveryOrderTask(dispatcher, iterator);

        mockServer.expect(ExpectedCount.never(),
                        MockRestRequestMatchers.requestTo(new URI(OrderDispatcher.ORDERS_RESOURCE)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.OK));
        //when
        underTest.run();

        //verify
        mockServer.verify();

        //then
        OrderDispatcher.OrderDispatcherStatus actual = dispatcher.totalOrders();
        Assertions.assertThat(actual.failures).isEqualTo(beforeExecution.failures);
        Assertions.assertThat(actual.success).isEqualTo(beforeExecution.success);
    }

    @Test
    public void shouldFail2Out2Orders_notSuccess() throws URISyntaxException {
        //given
        OrderDispatcher dispatcher = new OrderDispatcher(this.restTemplate);
        OrderDispatcher.OrderDispatcherStatus beforeExecution = dispatcher.totalOrders();

        Iterator<DeliveryOrderRequest> iterator = TestUtils.getOrders(2).iterator();
        this.underTest = new PostDeliveryOrderTask(dispatcher, iterator);

        int expectedExecutions = 2;
        mockServer.expect(ExpectedCount.times(expectedExecutions),
                        MockRestRequestMatchers.requestTo(new URI(OrderDispatcher.ORDERS_RESOURCE)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.BAD_REQUEST));
        //when
        underTest.run();

        //verify
        mockServer.verify();

        //then
        OrderDispatcher.OrderDispatcherStatus actual = dispatcher.totalOrders();
        Assertions.assertThat(actual.failures).isEqualTo(beforeExecution.failures + expectedExecutions);
        Assertions.assertThat(actual.success).isEqualTo(beforeExecution.success);
    }

    @Test
    public void shouldCompleteOneOrder() throws URISyntaxException {
        //given
        OrderDispatcher dispatcher = new OrderDispatcher(this.restTemplate);
        OrderDispatcher.OrderDispatcherStatus beforeExecution = dispatcher.totalOrders();

        Iterator<DeliveryOrderRequest> iterator = TestUtils.getOrders(1).iterator();
        this.underTest = new PostDeliveryOrderTask(dispatcher, iterator);
        mockServer.expect(ExpectedCount.times(1),
                        MockRestRequestMatchers.requestTo(new URI(OrderDispatcher.ORDERS_RESOURCE)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.OK));
        //when
        underTest.run();

        //verify
        mockServer.verify();

        //then
        OrderDispatcher.OrderDispatcherStatus actual = dispatcher.totalOrders();
        Assertions.assertThat(actual.failures).isEqualTo(beforeExecution.failures);
        Assertions.assertThat(actual.success).isEqualTo(beforeExecution.success + 1L);
    }

}