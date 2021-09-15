package com.acabra.orderfullfilment.orderserver.core.executor;

import com.acabra.orderfullfilment.orderserver.event.OutputEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.Deque;
import java.util.function.Consumer;

@Slf4j
public class OutputEventHandler extends SafeTask {
    private final Deque<OutputEvent> deque;
    private final Consumer<OutputEvent> dispatch;

    public OutputEventHandler(final Deque<OutputEvent> deque, Consumer<OutputEvent> dispatch) {
        this.deque = deque;
        this.dispatch = dispatch;
    }

    @Override
    protected void doWork() {
        OutputEvent take = deque.poll();
        if(take != null) {
            this.dispatch.accept(take);
        }
    }
}