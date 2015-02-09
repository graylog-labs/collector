package com.graylog.agent.outputs.gelf;

import com.google.inject.assistedinject.Assisted;
import com.graylog.agent.Message;
import com.graylog.agent.annotations.AgentOutputFactory;
import com.graylog.agent.buffer.Buffer;
import com.graylog.agent.outputs.OutputService;
import com.graylog.agent.config.ConfigurationUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class GelfOutput extends OutputService {
    private static final Logger LOG = LoggerFactory.getLogger(GelfOutput.class);

    @AgentOutputFactory
    public interface Factory extends OutputService.Factory<GelfOutput, GelfOutputConfiguration> {
        GelfOutput create(GelfOutputConfiguration configuration);
    }

    private final GelfOutputConfiguration configuration;
    private final Buffer buffer;

    private final CountDownLatch stopLatch = new CountDownLatch(1);

    @Inject
    public GelfOutput(@Assisted GelfOutputConfiguration configuration, Buffer buffer) {
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
    public void write(Message message) {
        LOG.info("[{}] Writing {}", getClass().getSimpleName(), message);
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
