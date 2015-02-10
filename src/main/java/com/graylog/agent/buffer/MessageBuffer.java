package com.graylog.agent.buffer;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.collect.Queues;
import com.graylog.agent.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.BlockingQueue;

import static com.codahale.metrics.MetricRegistry.name;

public class MessageBuffer implements Buffer {
    private static final Logger LOG = LoggerFactory.getLogger(MessageBuffer.class);

    private final BlockingQueue<Message> queue;
    private final MetricRegistry metricRegistry;
    private final Meter inserted;
    private final Meter removed;

    @Inject
    public MessageBuffer(MessageBufferConfiguration config, MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
        this.queue = Queues.newLinkedBlockingQueue(config.getSize());

        this.inserted = metricRegistry.meter(name(getClass(), "inserted"));
        this.removed = metricRegistry.meter(name(getClass(), "removed"));
    }

    public void insert(Message message) {
        LOG.debug("Adding message to queue: {}", message);

        try {
            queue.put(message);
            inserted.mark();
        } catch (InterruptedException e) {
            LOG.error("Interrupted, dropping message.", e);
        }
    }

    @Override
    public Message remove() {
        try {
            final Message message = queue.take();

            if (message != null) {
                removed.mark();
            }

            return message;
        } catch (InterruptedException e) {
            LOG.error("Interrupted while removing from buffer.", e);
            return null;
        }
    }
}
