package com.acabra.orderfullfilment.orderproducer.dispatch;

import com.acabra.orderfullfilment.orderproducer.config.RestClientConfig;
import com.acabra.orderfullfilment.orderproducer.dto.DeliveryOrderRequestDTO;
import com.acabra.orderfullfilment.orderproducer.dto.OrderDispatcherStatusPOJO;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.AfterEach;
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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.LongAdder;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {RestClientConfig.class})
class PostDeliveryOrderTaskTest {

    @Autowired
    private RestTemplate restTemplate;
    private PeriodicOrderDispatcherClientImpl dispatcherMock;
    private MockRestServiceServer mockServer;
    private final DeliveryOrderRequestDTO orderStub = new DeliveryOrderRequestDTO("1", "1", 1);

    @BeforeEach
    public void setup() {
        this.dispatcherMock = Mockito.mock(PeriodicOrderDispatcherClientImpl.class);
        this.mockServer = MockRestServiceServer.createServer(this.restTemplate);
    }

    @AfterEach
    public void tearDown() {
        Mockito.verifyNoMoreInteractions(this.dispatcherMock);
        this.dispatcherMock = null;
        this.mockServer = null;
    }

    @Test
    public void shouldFail_errorWithServer() throws URISyntaxException, ExecutionException, InterruptedException {
        //given
        Mockito.doNothing().when(dispatcherMock).reportWorkCompleted();
        CompletableFuture<Void> shutDownRequested = new CompletableFuture<>();

        LongAdder successCount = new LongAdder();
        LongAdder failureCount = new LongAdder();
        PostDeliveryOrderTask  underTest = new PostDeliveryOrderTask(() -> shutDownRequested.complete(null),
                orderStub, successCount, failureCount, this.restTemplate);
        mockServer.expect(ExpectedCount.times(1),
                        MockRestRequestMatchers.requestTo(new URI(PeriodicOrderDispatcherClientImpl.ORDERS_RESOURCE)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond((r) -> {
                    throw new RuntimeException("error");
                });
        //when
        CompletableFuture<Void> taskCompletedFuture = underTest.getTaskCompletedFuture();
        CompletableFuture.runAsync(underTest);
        taskCompletedFuture.get();

        //verify
        mockServer.verify();
        Mockito.verifyNoInteractions(dispatcherMock);

        //then
        Assertions.assertThat(shutDownRequested.isDone()).isTrue(); // abnormal completion, exception raised
        Assertions.assertThat(successCount.sum()).isEqualTo(0);
        Assertions.assertThat(failureCount.sum()).isEqualTo(1);
    }

    @Test
    public void shouldFailNullPointerException() {
        //given
        DeliveryOrderRequestDTO nullDeliveryRequest = null;
        //when
        LongAdder successCount = new LongAdder();
        LongAdder failureCount = new LongAdder();
        ThrowableAssert.ThrowingCallable throwingCallable = () -> new PostDeliveryOrderTask(dispatcherMock::reportWorkCompleted,
                nullDeliveryRequest, successCount, failureCount, this.restTemplate);

        //then
        Mockito.verifyNoInteractions(dispatcherMock);
        Assertions.assertThatThrownBy(throwingCallable)
                        .isExactlyInstanceOf(NullPointerException.class);
        //verify
        Mockito.verifyNoInteractions(dispatcherMock);
        Assertions.assertThat(successCount.sum()).isEqualTo(0);
        Assertions.assertThat(failureCount.sum()).isEqualTo(0);
    }

    @Test
    public void shouldSucceedExecutionOrder_noSigPillEncountered() throws URISyntaxException, ExecutionException, InterruptedException {
        //given
        Mockito.doNothing().when(dispatcherMock).reportWorkCompleted();
        CompletableFuture<Void> shutDownRequested = new CompletableFuture<>();

        LongAdder successCount = new LongAdder();
        LongAdder failureCount = new LongAdder();
        PostDeliveryOrderTask  underTest = new PostDeliveryOrderTask(() -> shutDownRequested.complete(null),
                orderStub, successCount, failureCount, this.restTemplate);
        mockServer.expect(ExpectedCount.times(1),
                        MockRestRequestMatchers.requestTo(new URI(PeriodicOrderDispatcherClientImpl.ORDERS_RESOURCE)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.CREATED));
        //when
        CompletableFuture<Void> taskCompletedFuture = underTest.getTaskCompletedFuture();
        CompletableFuture.runAsync(underTest);
        taskCompletedFuture.get();

        //verify
        mockServer.verify();
        Mockito.verifyNoInteractions(dispatcherMock);

        //then
        Assertions.assertThat(shutDownRequested.isDone()).isFalse(); // normal completion, no sigpill encountered
    }

    @Test
    public void shouldFailExecutionOrder_serverInternalError() throws URISyntaxException, ExecutionException, InterruptedException {
        //given
        Mockito.doNothing().when(dispatcherMock).reportWorkCompleted();
        CompletableFuture<Void> shutDownRequested = new CompletableFuture<>();

        LongAdder successCount = new LongAdder();
        LongAdder failureCount = new LongAdder();
        PostDeliveryOrderTask  underTest = new PostDeliveryOrderTask(() -> shutDownRequested.complete(null),
                orderStub, successCount, failureCount, this.restTemplate);
        mockServer.expect(ExpectedCount.times(1),
                        MockRestRequestMatchers.requestTo(new URI(PeriodicOrderDispatcherClientImpl.ORDERS_RESOURCE)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.INTERNAL_SERVER_ERROR).body(""));

        //when
        CompletableFuture<Void> taskCompletedFuture = underTest.getTaskCompletedFuture();
        CompletableFuture.runAsync(underTest);
        taskCompletedFuture.get();

        //verify
        mockServer.verify();
        Mockito.verifyNoInteractions(dispatcherMock);

        //then
        Assertions.assertThat(shutDownRequested.isDone()).isTrue(); // abnormal completion, server internal error
        Assertions.assertThat(successCount.sum()).isEqualTo(0);
        Assertions.assertThat(failureCount.sum()).isEqualTo(1);
    }

    @Test
    public void shouldCompleteExecutionOrder_badRequest() throws URISyntaxException, ExecutionException, InterruptedException {
        //given
        Mockito.doNothing().when(dispatcherMock).reportWorkCompleted();
        CompletableFuture<Void> shutDownRequested = new CompletableFuture<>();
        LongAdder successCount = new LongAdder();
        LongAdder failureCount = new LongAdder();
        PostDeliveryOrderTask  underTest = new PostDeliveryOrderTask(() -> shutDownRequested.complete(null),
                orderStub, successCount, failureCount, this.restTemplate);
        mockServer.expect(ExpectedCount.times(1),
                        MockRestRequestMatchers.requestTo(new URI(PeriodicOrderDispatcherClientImpl.ORDERS_RESOURCE)))
                .andExpect(MockRestRequestMatchers.method(HttpMethod.POST))
                .andRespond(MockRestResponseCreators.withStatus(HttpStatus.BAD_REQUEST).body("{\"message\": \"something\"}"));

        //when
        CompletableFuture<Void> taskCompletedFuture = underTest.getTaskCompletedFuture();
        CompletableFuture.runAsync(underTest);
        taskCompletedFuture.get();

        //verify
        mockServer.verify();
        Mockito.verifyNoInteractions(dispatcherMock);

        //then
        Assertions.assertThat(shutDownRequested.isDone()).isFalse(); // normal completion, bad request
        Assertions.assertThat(successCount.sum()).isEqualTo(0);
        Assertions.assertThat(failureCount.sum()).isEqualTo(1);
    }

    @Test
    public void shouldCompleteRequest_sigPillGiven() throws ExecutionException, InterruptedException {
        //given
        CompletableFuture<Void> shutDownRequested = new CompletableFuture<>();
        LongAdder successCount = new LongAdder();
        LongAdder failureCount = new LongAdder();
        PostDeliveryOrderTask  underTest = new PostDeliveryOrderTask(() -> shutDownRequested.complete(null),
                DeliveryOrderRequestDTO.ofSigPill(), successCount, failureCount, this.restTemplate);

        //when
        CompletableFuture<Void> taskCompletedFuture = underTest.getTaskCompletedFuture();
        CompletableFuture.runAsync(underTest);
        taskCompletedFuture.get();

        Mockito.verifyNoInteractions(dispatcherMock);

        //then
        Assertions.assertThat(shutDownRequested.isDone()).isTrue(); // abnormal completion, sig pill
        Assertions.assertThat(successCount.sum()).isEqualTo(0);
        Assertions.assertThat(failureCount.sum()).isEqualTo(0);
    }
}