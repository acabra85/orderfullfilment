package com.acabra.orderfullfilment.orderproducer.dispatch;

import com.acabra.orderfullfilment.orderproducer.dto.OrderDispatcherStatusPOJO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.concurrent.CompletableFuture;

@ExtendWith(SpringExtension.class)
class CommandLineRunnerImplTest {

    private static final CompletableFuture<OrderDispatcherStatusPOJO> DEFAULT_FUTURE_RETURN
            = CompletableFuture.completedFuture(OrderDispatcherStatusPOJO.of(5, 0));


    private CommandLineRunnerImpl underTest;
    private PeriodicOrderDispatcherClient dispatcherMock = Mockito.mock(PeriodicOrderDispatcherClientImpl.class);

    final ResourceLoader resourceLoader;

    CommandLineRunnerImplTest(@Autowired  ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @BeforeEach
    public void setup() {
        underTest = new CommandLineRunnerImpl(dispatcherMock, resourceLoader);
    }

    @AfterEach
    public void tearDown() {
        Mockito.verifyNoMoreInteractions(dispatcherMock);
    }


    @Test
    public void mustCompleteRunWithDefaults_givenNullAsArgument() throws Exception {
        //given
        Mockito.doReturn(DEFAULT_FUTURE_RETURN).when(dispatcherMock).getCompletionFuture();
        Mockito.doNothing().when(dispatcherMock).dispatchTwoOrdersPerSecond(Mockito.anyList());
        String[] args = null;

        //wnen
        underTest.run(args);

        //then

        //verify
        Mockito.verify(dispatcherMock, Mockito.times(1)).getCompletionFuture();
        Mockito.verify(dispatcherMock, Mockito.times(1)).dispatchTwoOrdersPerSecond(Mockito.anyList());
    }

    @Test
    public void mustCompleteRunWithDefaults_givenEmptyParameters() throws Exception {
        //given
        Mockito.doReturn(DEFAULT_FUTURE_RETURN).when(dispatcherMock).getCompletionFuture();
        Mockito.doNothing().when(dispatcherMock).dispatchTwoOrdersPerSecond(Mockito.anyList());
        String[] args = {};

        //wnen
        underTest.run(args);

        //verify
        Mockito.verify(dispatcherMock, Mockito.times(1)).getCompletionFuture();
        Mockito.verify(dispatcherMock, Mockito.times(1)).dispatchTwoOrdersPerSecond(Mockito.anyList());
    }

    @Test
    public void mustCompleteRunWithDefaults_givenNullArgument() throws Exception {
        //given
        Mockito.doReturn(DEFAULT_FUTURE_RETURN).when(dispatcherMock).getCompletionFuture();
        Mockito.doNothing().when(dispatcherMock).dispatchTwoOrdersPerSecond(Mockito.anyList());
        String[] args = {null};

        //wnen
        underTest.run(args);

        //verify
        Mockito.verify(dispatcherMock, Mockito.times(1)).getCompletionFuture();
        Mockito.verify(dispatcherMock, Mockito.times(1)).dispatchTwoOrdersPerSecond(Mockito.anyList());
    }

    @Test
    public void mustCompleteRunWithDefaults_givenEmptyStringArgument() throws Exception {
        //given
        Mockito.doReturn(DEFAULT_FUTURE_RETURN).when(dispatcherMock).getCompletionFuture();
        Mockito.doNothing().when(dispatcherMock).dispatchTwoOrdersPerSecond(Mockito.anyList());
        String[] args = {""};

        //wnen
        underTest.run(args);

        //verify
        Mockito.verify(dispatcherMock, Mockito.times(1)).getCompletionFuture();
        Mockito.verify(dispatcherMock, Mockito.times(1)).dispatchTwoOrdersPerSecond(Mockito.anyList());
    }

    @Test
    public void mustCompleteRun_givenTinyParameter() throws Exception {
        //given
        Mockito.doReturn(DEFAULT_FUTURE_RETURN).when(dispatcherMock).getCompletionFuture();
        Mockito.doNothing().when(dispatcherMock).dispatchTwoOrdersPerSecond(Mockito.anyList());
        String[] args = {"tiny"};

        //when
        underTest.run(args);

        //verify
        Mockito.verify(dispatcherMock, Mockito.times(1)).getCompletionFuture();
        Mockito.verify(dispatcherMock, Mockito.times(1)).dispatchTwoOrdersPerSecond(Mockito.anyList());
    }

    @Test
    public void mustCompleteRun_givenSmallParameter() throws Exception {
        //given
        Mockito.doReturn(DEFAULT_FUTURE_RETURN).when(dispatcherMock).getCompletionFuture();
        Mockito.doNothing().when(dispatcherMock).dispatchTwoOrdersPerSecond(Mockito.anyList());
        String[] args = {"small"};

        //when
        underTest.run(args);

        //verify
        Mockito.verify(dispatcherMock, Mockito.times(1)).getCompletionFuture();
        Mockito.verify(dispatcherMock, Mockito.times(1)).dispatchTwoOrdersPerSecond(Mockito.anyList());
    }

    @Test
    public void mustCompleteRun_givenLargeFileParameter() throws Exception {
        //given
        CompletableFuture<OrderDispatcherStatusPOJO> completionFuture = CompletableFuture.completedFuture(
                OrderDispatcherStatusPOJO.of(5, 0)
        );
        Mockito.doReturn(completionFuture).when(dispatcherMock).getCompletionFuture();
        Mockito.doNothing().when(dispatcherMock).dispatchTwoOrdersPerSecond(Mockito.anyList());
        String[] args = {"large"};

        //when
        underTest.run(args);

        //verify
        Mockito.verify(dispatcherMock, Mockito.times(1)).getCompletionFuture();
        Mockito.verify(dispatcherMock, Mockito.times(1)).dispatchTwoOrdersPerSecond(Mockito.anyList());
    }

    @Test
    public void mustCompleteRun_givenInvalidSpecifiedFile() throws Exception {
        //given
        CompletableFuture<OrderDispatcherStatusPOJO> completionFuture = CompletableFuture.completedFuture(
                OrderDispatcherStatusPOJO.of(5, 0)
        );
        Mockito.doReturn(completionFuture).when(dispatcherMock).getCompletionFuture();
        Mockito.doNothing().when(dispatcherMock).dispatchTwoOrdersPerSecond(Mockito.anyList());
        String[] args = {resourceLoader.getResource("classpath:my-existent-invalid.json").getFile().getAbsolutePath()};

        //when
        underTest.run(args);

        //verify
        Mockito.verify(dispatcherMock, Mockito.times(1)).getCompletionFuture();
        Mockito.verify(dispatcherMock, Mockito.times(1)).dispatchTwoOrdersPerSecond(Mockito.anyList());
    }

    @Test
    public void mustCompleteRun_givenValidSpecifiedFile() throws Exception {
        //given
        CompletableFuture<OrderDispatcherStatusPOJO> completionFuture = CompletableFuture.completedFuture(
                OrderDispatcherStatusPOJO.of(5, 0)
        );
        Mockito.doReturn(completionFuture).when(dispatcherMock).getCompletionFuture();
        Mockito.doNothing().when(dispatcherMock).dispatchTwoOrdersPerSecond(Mockito.anyList());
        String[] args = {resourceLoader.getResource("classpath:my-existent-valid.json").getFile().getAbsolutePath()};

        //when
        underTest.run(args);

        //verify
        Mockito.verify(dispatcherMock, Mockito.times(1)).getCompletionFuture();
        Mockito.verify(dispatcherMock, Mockito.times(1)).dispatchTwoOrdersPerSecond(Mockito.anyList());
    }

    @Test
    public void mustCompleteRun_givenNonExistentSpecifiedFile() throws Exception {
        //given
        Mockito.doReturn(DEFAULT_FUTURE_RETURN).when(dispatcherMock).getCompletionFuture();
        Mockito.doNothing().when(dispatcherMock).dispatchTwoOrdersPerSecond(Mockito.anyList());
        String[] args = {"my-definitely-non-existent-file.json"};

        //when
        underTest.run(args);

        //verify
        Mockito.verify(dispatcherMock, Mockito.times(1)).getCompletionFuture();
        Mockito.verify(dispatcherMock, Mockito.times(1)).dispatchTwoOrdersPerSecond(Mockito.anyList());
    }


}