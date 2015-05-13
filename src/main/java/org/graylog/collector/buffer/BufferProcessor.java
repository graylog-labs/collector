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
package org.graylog.collector.buffer;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import org.graylog.collector.Message;
import org.graylog.collector.utils.CollectorId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Set;

public class BufferProcessor extends AbstractExecutionThreadService {
    private static final Logger LOG = LoggerFactory.getLogger(BufferProcessor.class);

    private final Buffer buffer;
    private final Set<BufferConsumer> consumers;
    private final CollectorId collectorId;
    private Thread thread;

    @Inject
    public BufferProcessor(Buffer buffer, Set<BufferConsumer> consumers, CollectorId collectorId) {
        this.buffer = buffer;
        this.consumers = consumers;
        this.collectorId = collectorId;
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
                // It is a bit disgusting to mutate the message object here, but for now the simplest solution for
                // adding common fields to it.
                decorateMessage(message);

                LOG.debug("Read message from buffer {}", message);

                for (final BufferConsumer consumer : consumers) {
                    LOG.debug("Processing message with consumer {}", consumer);
                    consumer.process(message);
                }
            }
        }
    }

    private void decorateMessage(Message message) {
        message.getFields().put("gl2_source_collector", collectorId.toString());
        message.getFields().put("gl2_source_collector_input", message.getInput());
    }
}
