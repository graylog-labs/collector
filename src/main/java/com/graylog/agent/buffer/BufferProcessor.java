/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
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
