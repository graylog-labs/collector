package com.graylog.agent.buffer;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.graylog.agent.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class BufferProcessor extends AbstractExecutionThreadService {
    private static final Logger LOG = LoggerFactory.getLogger(BufferProcessor.class);

    private final Buffer buffer;
    private final Set<BufferConsumer> consumers;

    public BufferProcessor(Buffer buffer, Set<BufferConsumer> consumers) {
        this.buffer = buffer;
        this.consumers = consumers;
    }

    @Override
    protected void run() throws Exception {
        while (isRunning()) {
            final Message message = buffer.remove();

            if (message != null) {
                LOG.info("Read message from buffer {}", message);

                for (final BufferConsumer consumer : consumers) {
                    LOG.info("Processing message with consumer {}", consumer);
                    consumer.process(message);
                }
            }
        }
    }
}
