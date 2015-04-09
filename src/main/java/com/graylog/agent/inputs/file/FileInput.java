package com.graylog.agent.inputs.file;

import com.google.inject.assistedinject.Assisted;
import com.graylog.agent.MessageBuilder;
import com.graylog.agent.annotations.AgentInputFactory;
import com.graylog.agent.buffer.Buffer;
import com.graylog.agent.config.ConfigurationUtils;
import com.graylog.agent.file.ChunkReader;
import com.graylog.agent.file.FileReaderService;
import com.graylog.agent.file.naming.NumberSuffixStrategy;
import com.graylog.agent.inputs.InputService;
import com.graylog.agent.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class FileInput extends InputService {
    public enum InitialReadPosition {
        START,
        END;
    }

    @AgentInputFactory
    public interface Factory extends InputService.Factory<FileInput, FileInputConfiguration> {
        FileInput create(FileInputConfiguration configuration);
    }

    private static final Logger LOG = LoggerFactory.getLogger(FileInput.class);

    private final FileInputConfiguration configuration;
    private final Buffer buffer;
    private final CountDownLatch stopLatch = new CountDownLatch(1);

    @Inject
    public FileInput(@Assisted FileInputConfiguration inputConfiguration, Buffer buffer) {
        this.configuration = inputConfiguration;
        this.buffer = buffer;
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
    protected void run() throws Exception {
        final Path path = configuration.getPath().toPath();
        final MessageBuilder messageBuilder = new MessageBuilder().input(getId()).outputs(getOutputs()).source(Utils.getHostname());
        final FileReaderService readerService = new FileReaderService(
                path,
                new NumberSuffixStrategy(path),
                true,
                InitialReadPosition.END,
                this,
                messageBuilder,
                configuration.createContentSplitter(),
                buffer
        );

        readerService.startAsync();
        readerService.awaitRunning();

        stopLatch.await();

        readerService.stopAsync().awaitTerminated();
    }

    @Override
    protected void triggerShutdown() {
        stopLatch.countDown();
    }

    @Override
    public void setReaderFinished(ChunkReader chunkReader) {
        // TODO Check if needed and for what it was used.
    }

    @Override
    public String toString() {
        return ConfigurationUtils.toString(configuration, this);
    }
}
