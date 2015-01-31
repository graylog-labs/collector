/*
 * Copyright 2014 TORCH GmbH
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.graylog.agent.file;

import com.google.common.collect.Queues;
import com.graylog.agent.file.compat.Buffer;
import com.graylog.agent.file.compat.Message;
import com.graylog.agent.file.compat.MessageInput;

import java.util.concurrent.BlockingQueue;

class CollectingBuffer extends Buffer {

    private final BlockingQueue<Message> messages = Queues.newArrayBlockingQueue(16);
    private boolean outOfCapacity = false;
    private boolean processingDisabled = false;

    @Override
    public void insertFailFast(Message message, MessageInput sourceInput) {
        messages.add(message);
    }

    @Override
    public void insertCached(Message message, MessageInput sourceInput) {
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
