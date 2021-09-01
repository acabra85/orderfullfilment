package com.acabra.orderfullfilment.orderproducer.dispatch;

import com.acabra.orderfullfilment.orderproducer.config.RestClientConfig;
import com.acabra.orderfullfilment.orderproducer.dto.DeliveryOrderRequest;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
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

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RestClientConfig.class})
class PostDeliveryOrderTaskTest {

    private PostDeliveryOrderTask underTest;

    @Autowired
    private RestTemplate restTemplate;
    private MockRestServiceServer mockServer;
    private final DeliveryOrderRequest orderStub = new DeliveryOrderRequest("1", "1", 1);

    @BeforeEach
    public void setup() {
        this.mockServer = MockRestServiceServer.createServer(this.restTemplate);
    }

    @Test
    public void shouldFail_errorWithServer() throws URISyntaxException {
        //given
        PeriodicOrderDispatcherClientImpl dispatcher = new PeriodicOrderDispatcherClientImpl(this.restTemplate);
        PeriodicOrderDispatcherClientImpl.OrderDispatcherStatus beforeExecution = dispatcher.totalOrders();

        this.underTest = new PostDeliveryOrderTask(dispatcher, orderStub);
        mockServer.expect(ExpectedCount.times(1),
                        MockRestRequestMatchers.requestTo(new URI(PeriodicOrderDispatcherClientImpl.ORDERS_RESOURCE)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond((r) -> {
                    throw new RuntimeException("error");
                });
        //when
        underTest.run();

        //verify
        mockServer.verify();

        //then
        PeriodicOrderDispatcherClientImpl.OrderDispatcherStatus actual = dispatcher.totalOrders();
        Assertions.assertThat(actual.failures).isEqualTo(beforeExecution.failures + 1L);
        Assertions.assertThat(actual.success).isEqualTo(beforeExecution.success);
    }

    @Test
    public void shouldFailNullPointerException() throws URISyntaxException {
        //given
        DeliveryOrderRequest deliveryOrderRequest = null;
        PeriodicOrderDispatcherClientImpl dispatcherMock = Mockito.mock(PeriodicOrderDispatcherClientImpl.class);

        //when
        ThrowableAssert.ThrowingCallable throwingCallable = () -> new PostDeliveryOrderTask(dispatcherMock,
                deliveryOrderRequest);

        //then
        Mockito.verifyNoInteractions(dispatcherMock);
        Assertions.assertThatThrownBy(throwingCallable)
                        .isExactlyInstanceOf(NullPointerException.class);
    }

    @Test
    public void shouldSucceedExecutionOrder() throws URISyntaxException {
        //given
        PeriodicOrderDispatcherClientImpl dispatcher = new PeriodicOrderDispatcherClientImpl(this.restTemplate);
        PeriodicOrderDispatcherClientImpl.OrderDispatcherStatus beforeExecution = dispatcher.totalOrders();

        this.underTest = new PostDeliveryOrderTask(dispatcher, orderStub);
        mockServer.expect(ExpectedCount.times(1),
                        MockRestRequestMatchers.requestTo(new URI(PeriodicOrderDispatcherClientImpl.ORDERS_RESOURCE)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.CREATED));
        //when
        underTest.run();

        //verify
        mockServer.verify();

        //then
        PeriodicOrderDispatcherClientImpl.OrderDispatcherStatus actual = dispatcher.totalOrders();
        Assertions.assertThat(actual.failures).isEqualTo(beforeExecution.failures);
        Assertions.assertThat(actual.success).isEqualTo(beforeExecution.success + 1);
    }

    @Test
    public void shouldFailExecutionOrder_BadRequest() throws URISyntaxException {
        //given
        PeriodicOrderDispatcherClientImpl dispatcher = new PeriodicOrderDispatcherClientImpl(this.restTemplate);
        PeriodicOrderDispatcherClientImpl.OrderDispatcherStatus beforeExecution = dispatcher.totalOrders();

        this.underTest = new PostDeliveryOrderTask(dispatcher, orderStub);
        mockServer.expect(ExpectedCount.times(1),
                        MockRestRequestMatchers.requestTo(new URI(PeriodicOrderDispatcherClientImpl.ORDERS_RESOURCE)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.BAD_REQUEST).body(""));
        //when
        underTest.run();

        //verify
        mockServer.verify();

        //then
        PeriodicOrderDispatcherClientImpl.OrderDispatcherStatus actual = dispatcher.totalOrders();
        Assertions.assertThat(actual.failures).isEqualTo(beforeExecution.failures + 1);
        Assertions.assertThat(actual.success).isEqualTo(beforeExecution.success);
    }

    @Test
    public void shouldRequestShutDownSigPill() throws URISyntaxException {
        //given
        PeriodicOrderDispatcherClientImpl dispatcher = new PeriodicOrderDispatcherClientImpl(this.restTemplate);
        PeriodicOrderDispatcherClientImpl.OrderDispatcherStatus beforeExecution = dispatcher.totalOrders();

        this.underTest = new PostDeliveryOrderTask(dispatcher, DeliveryOrderRequest.ofSigPill());

        //when
        underTest.run();

        //verify

        //then
        PeriodicOrderDispatcherClientImpl.OrderDispatcherStatus actual = dispatcher.totalOrders();
        Assertions.assertThat(actual.failures).isEqualTo(beforeExecution.failures);
        Assertions.assertThat(actual.success).isEqualTo(beforeExecution.success);
    }
}