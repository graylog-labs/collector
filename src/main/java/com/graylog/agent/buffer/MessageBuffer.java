package com.graylog.agent.buffer;

import com.google.common.collect.Queues;
import com.graylog.agent.Message;
import com.graylog.agent.inputs.Input;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

public class MessageBuffer implements Buffer {
    private static final Logger LOG = LoggerFactory.getLogger(MessageBuffer.class);

    private final BlockingQueue<Message> queue;

    public MessageBuffer(final int capacity) {
        this.queue = Queues.newLinkedBlockingQueue(capacity);
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
