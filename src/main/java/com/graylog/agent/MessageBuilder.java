package com.graylog.agent;

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
    private String input;
    private Set<String> outputs;
    private MessageFields fields;

    public MessageBuilder() {
    }

    private MessageBuilder(String message, String source, DateTime timestamp, String input, Set<String> outputs, MessageFields fields) {
        this.message = message;
        this.source = source;
        this.timestamp = timestamp;
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

    public MessageBuilder input(String input) {
        checkOwnership();
        this.input = input;
        return this;
    }

    public MessageBuilder outputs(Set<String> outputs) {
        checkOwnership();
        this.outputs = ImmutableSet.copyOf(outputs);
        return this;
    }

    public MessageBuilder fields(MessageFields fields) {
        checkOwnership();
        this.fields = fields;
        return this;
    }

    public Message build() {
        checkNotNull(message, "Message should not be null!");
        checkNotNull(source, "Message source should not be null!");
        checkNotNull(timestamp, "Message timestamp should not be null!");
        checkNotNull(input, "Message input should not be null!");
        checkNotNull(outputs, "Message outputs should not be null!");

        if (fields == null) {
            return new Message(message, source, timestamp, input, outputs);
        } else {
            return new Message(message, source, timestamp, input, outputs, fields);
        }
    }

    public MessageBuilder copy() {
        return new MessageBuilder(message, source, timestamp, input, outputs, fields);
    }

    private void checkOwnership() {
        final long currentId = Thread.currentThread().getId();
        checkState(ownerId == currentId, "Modification only allowed by owning thread. (owner=" + ownerId + " current=" + currentId + ")");
    }
}
