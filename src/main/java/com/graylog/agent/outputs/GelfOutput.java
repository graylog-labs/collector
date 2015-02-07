package com.graylog.agent.outputs;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.graylog.agent.buffer.Buffer;
import com.graylog.agent.utils.ConfigurationUtils;

import java.util.concurrent.CountDownLatch;

public class GelfOutput extends AbstractExecutionThreadService implements Output {
    private final GelfOutputConfiguration configuration;
    private final Buffer buffer;

    private final CountDownLatch stopLatch = new CountDownLatch(1);

    public GelfOutput(GelfOutputConfiguration configuration, Buffer buffer) {
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

    @Override
    public String toString() {
        return ConfigurationUtils.toString(configuration, this);
    }
}
