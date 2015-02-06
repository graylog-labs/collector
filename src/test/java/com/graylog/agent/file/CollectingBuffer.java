package com.graylog.agent.file;

import com.google.common.collect.Queues;
import com.graylog.agent.file.compat.Buffer;
import com.graylog.agent.file.compat.Message;
import com.graylog.agent.file.compat.MessageInput;

import java.util.concurrent.BlockingQueue;

class CollectingBuffer implements Buffer {

    private final BlockingQueue<Message> messages = Queues.newArrayBlockingQueue(16);
    private boolean outOfCapacity = false;
    private boolean processingDisabled = false;

    @Override
    public void insert(Message message, MessageInput sourceInput) {
        messages.add(message);
    }

    public boolean isOutOfCapacity() {
        return outOfCapacity;
    }

    public void setOutOfCapacity(boolean outOfCapacity) {
        this.outOfCapacity = outOfCapacity;
    }

    public boolean isProcessingDisabled() {
        return processingDisabled;
    }

    public void setProcessingDisabled(boolean processingDisabled) {
        this.processingDisabled = processingDisabled;
    }

    public BlockingQueue<Message> getMessageQueue() {
        return messages;
    }

}
