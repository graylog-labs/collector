package com.graylog.agent.buffer;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.graylog.agent.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Set;

public class BufferProcessor extends AbstractExecutionThreadService {
    private static final Logger LOG = LoggerFactory.getLogger(BufferProcessor.class);

    private final Buffer buffer;
    private final Set<BufferConsumer> consumers;
    private Thread thread;

    @Inject
    public BufferProcessor(Buffer buffer, Set<BufferConsumer> consumers) {
        this.buffer = buffer;
        this.consumers = consumers;
    }

    @Override
    protected void startUp() throws Exception {
        this.thread = Thread.currentThread();
    }

    @Override
    protected void triggerShutdown() {
        thread.interrupt();
    }

    @Override
    protected void run() throws Exception {
        while (isRunning()) {
            final Message message = buffer.remove();

            if (message != null) {
                LOG.debug("Read message from buffer {}", message);

                for (final BufferConsumer consumer : consumers) {
                    LOG.debug("Processing message with consumer {}", consumer);
                    consumer.process(message);
                }
            }
        }
    }
}
