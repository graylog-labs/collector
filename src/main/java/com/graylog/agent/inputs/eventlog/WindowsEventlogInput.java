package com.graylog.agent.inputs.eventlog;

import com.google.inject.assistedinject.Assisted;
import com.graylog.agent.MessageBuilder;
import com.graylog.agent.buffer.Buffer;
import com.graylog.agent.config.ConfigurationUtils;
import com.graylog.agent.file.ChunkReader;
import com.graylog.agent.inputs.InputService;
import org.hyperic.sigar.win32.EventLogThread;

import javax.inject.Inject;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class WindowsEventlogInput extends InputService {
    private final WindowsEventlogInputConfiguration configuration;
    private final Buffer buffer;
    private final CountDownLatch stopLatch = new CountDownLatch(1);

    public interface Factory extends InputService.Factory<WindowsEventlogInput, WindowsEventlogInputConfiguration> {
        WindowsEventlogInput create(WindowsEventlogInputConfiguration configuration);
    }

    @Inject
    public WindowsEventlogInput(@Assisted WindowsEventlogInputConfiguration configuration, Buffer buffer) {
        this.configuration = configuration;
        this.buffer = buffer;
    }

    @Override
    protected void triggerShutdown() {
        stopLatch.countDown();
    }

    @Override
    protected void run() throws Exception {
        final MessageBuilder messageBuilder = new MessageBuilder().input(getId()).outputs(getOutputs());
        final EventLogThread logThread = EventLogThread.getInstance(configuration.getSourceName());

        logThread.add(new WindowsEventlogHandler(messageBuilder, buffer));
        logThread.setInterval(configuration.getPollInterval());
        logThread.doStart();

        stopLatch.await();

        logThread.doStop();
    }

    @Override
    public String getId() {
        return configuration.getId();
    }

    @Override
    public Set<String> getOutputs() {
        return configuration.getOutputs();
    }

    @Override
    public void setReaderFinished(ChunkReader chunkReader) {

    }

    @Override
    public String toString() {
        return ConfigurationUtils.toString(configuration, this);
    }
}
