package com.graylog.agent.outputs;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.graylog.agent.buffer.Buffer;

import java.util.concurrent.CountDownLatch;

public class StdoutOutput extends AbstractExecutionThreadService implements Output {
    private final StdoutOutputConfiguration configuration;
    private final Buffer buffer;

    private final CountDownLatch stopLatch = new CountDownLatch(1);

    public StdoutOutput(StdoutOutputConfiguration configuration, Buffer buffer) {
        this.configuration = configuration;
        this.buffer = buffer;
    }

    @Override
    protected void triggerShutdown() {
        stopLatch.countDown();
    }

    @Override
    protected void run() throws Exception {
        stopLatch.await();
    }
}
