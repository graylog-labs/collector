package com.graylog.agent.inputs;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.graylog.agent.file.ChunkReader;
import com.graylog.agent.file.FileReaderService;
import com.graylog.agent.buffer.Buffer;
import com.graylog.agent.file.naming.NumberSuffixStrategy;
import com.graylog.agent.file.splitters.NewlineChunkSplitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

public class FileInput extends AbstractExecutionThreadService implements Input {
    public enum InitialReadPosition {
        START,
        END;
    }

    private static final Logger LOG = LoggerFactory.getLogger(FileInput.class);

    private final FileInputConfiguration configuration;
    private final Buffer buffer;
    private final CountDownLatch stopLatch = new CountDownLatch(0);

    public FileInput(FileInputConfiguration inputConfiguration, Buffer buffer) {
        this.configuration = inputConfiguration;
        this.buffer = buffer;
    }

    public String getId() {
        return configuration.getId();
    }

    @Override
    protected void run() throws Exception {
        LOG.info("Starting input [{}]", configuration);

        final Path path = configuration.getPath().toPath();
        final FileReaderService readerService = new FileReaderService(
                path,
                new NumberSuffixStrategy(path),
                true,
                InitialReadPosition.START,
                this,
                null,
                new NewlineChunkSplitter(),
                buffer
        );

        readerService.startAsync();
        readerService.awaitRunning();

        stopLatch.await();
    }

    @Override
    protected void triggerShutdown() {
        stopLatch.countDown();
    }

    @Override
    public void setReaderFinished(ChunkReader chunkReader) {
        // TODO Check if needed and for what it was used.
    }
}
