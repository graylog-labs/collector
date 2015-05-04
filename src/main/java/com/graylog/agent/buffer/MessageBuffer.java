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
            return null;
        }
    }
}
