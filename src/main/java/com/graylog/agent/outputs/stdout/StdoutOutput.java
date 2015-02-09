package com.graylog.agent.outputs.stdout;

import com.google.inject.assistedinject.Assisted;
import com.graylog.agent.annotations.AgentOutputFactory;
import com.graylog.agent.buffer.Buffer;
import com.graylog.agent.outputs.OutputService;
import com.graylog.agent.config.ConfigurationUtils;

import javax.inject.Inject;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class StdoutOutput extends OutputService {
    @AgentOutputFactory
    public interface Factory extends OutputService.Factory<StdoutOutput, StdoutOutputConfiguration> {
        StdoutOutput create(StdoutOutputConfiguration configuration);
    }

    private final StdoutOutputConfiguration configuration;
    private final Buffer buffer;

    private final CountDownLatch stopLatch = new CountDownLatch(1);

    @Inject
    public StdoutOutput(@Assisted StdoutOutputConfiguration configuration, Buffer buffer) {
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

    @Override
    public String getId() {
        return configuration.getId();
    }

    @Override
    public Set<String> getInputs() {
        return configuration.getInputs();
    }
}
