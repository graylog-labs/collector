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
package org.graylog.collector;

import com.google.common.collect.ImmutableSet;
import org.joda.time.DateTime;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

public class MessageBuilder {
    private final long ownerId = Thread.currentThread().getId();

    private String message;
    private String source;
    private DateTime timestamp;
    private Message.Level level;
    private String input;
    private Set<String> outputs;
    private MessageFields fields = new MessageFields();

    public MessageBuilder() {
    }

    private MessageBuilder(String message, String source, DateTime timestamp, Message.Level level, String input, Set<String> outputs, MessageFields fields) {
        this.message = message;
        this.source = source;
        this.timestamp = timestamp;
        this.level = level;
        this.input = input;
        this.outputs = outputs;
        this.fields = fields;
    }

    public MessageBuilder message(String message) {
        checkOwnership();
        this.message = message;
        return this;
    }

    public MessageBuilder source(String source) {
        checkOwnership();
        this.source = source;
        return this;
    }

    public MessageBuilder timestamp(DateTime timestamp) {
        checkOwnership();
        this.timestamp = timestamp;
        return this;
    }

    public MessageBuilder level(Message.Level level) {
        checkOwnership();
        this.level = level;
        return this;
    }

    public MessageBuilder input(String input) {
        checkOwnership();
        this.input = input;
        return this;
    }

    public MessageBuilder outputs(Set<String> outputs) {
        checkOwnership();
        if (outputs == null) {
            this.outputs = null;
        } else {
            this.outputs = ImmutableSet.copyOf(outputs);
        }
        return this;
    }

    public MessageBuilder fields(MessageFields fields) {
        checkOwnership();
        this.fields = fields;
        return this;
    }

    public MessageBuilder addField(String key, int value) {
        checkOwnership();
        checkNotNull(fields);
        this.fields.put(key, value);
        return this;
    }

    public MessageBuilder addField(String key, long value) {
        checkOwnership();
        checkNotNull(fields);
        this.fields.put(key, value);
        return this;
    }

    public MessageBuilder addField(String key, boolean value) {
        checkOwnership();
        checkNotNull(fields);
        this.fields.put(key, value);
        return this;
    }

    public MessageBuilder addField(String key, String value) {
        checkOwnership();
        checkNotNull(fields);
        this.fields.put(key, value);
        return this;
    }

    public Message build() {
        checkNotNull(message, "Message should not be null!");
        checkNotNull(source, "Message source should not be null!");
        checkNotNull(timestamp, "Message timestamp should not be null!");
        checkNotNull(input, "Message input should not be null!");
        checkNotNull(outputs, "Message outputs should not be null!");

        if (fields == null) {
            return new Message(message, source, timestamp, level, input, outputs);
        } else {
            return new Message(message, source, timestamp, level, input, outputs, fields);
        }
    }

    public MessageBuilder copy() {
        return new MessageBuilder(message, source, timestamp, level, input, outputs, fields.copy());
    }

    private void checkOwnership() {
        final long currentId = Thread.currentThread().getId();
        checkState(ownerId == currentId, "Modification only allowed by owning thread. (owner=" + ownerId + " current=" + currentId + ")");
    }
}
