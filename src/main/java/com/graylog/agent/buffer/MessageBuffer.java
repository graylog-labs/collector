package com.graylog.agent.buffer;

import com.google.common.collect.Queues;
import com.graylog.agent.Message;
import com.graylog.agent.inputs.Input;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.BlockingQueue;

public class MessageBuffer implements Buffer {
    private static final Logger LOG = LoggerFactory.getLogger(MessageBuffer.class);

    private final BlockingQueue<Message> queue;

    @Inject
    public MessageBuffer(MessageBufferConfiguration config) {
        this.queue = Queues.newLinkedBlockingQueue(config.getSize());
    }

    public void insert(Message message, Input input) {
        LOG.debug("Adding message to queue: {}", message);

        try {
            queue.put(message);
        } catch (InterruptedException e) {
            LOG.error("Interrupted, dropping message.", e);
        }
    }

    @Override
    public Message remove() {
        try {
            return queue.take();
        } catch (InterruptedException e) {
            LOG.error("Interrupted while removing from buffer.", e);
            return null;
        }
    }
}
