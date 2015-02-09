package com.graylog.agent;

import com.google.common.base.Joiner;
import org.joda.time.DateTime;

import java.util.Set;

public class Message {
    private final DateTime timestamp;
    private final String input;
    private final Set<String> outputs;
    private final MessageFields fields;
    private String message;
    private String source;

    public Message(String message, String source, DateTime timestamp, String input, Set<String> outputs) {
        this(message, source, timestamp, input, outputs, new MessageFields());
    }

    public Message(String message, String source, DateTime timestamp, String input, Set<String> outputs, MessageFields fields) {
        this.source = source;
        this.message = message;
        this.timestamp = timestamp;
        this.input = input;
        this.outputs = outputs;
        this.fields = fields;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    public String getSource() {
        return source;
    }

    public String getInput() {
        return input;
    }

    public Set<String> getOutputs() {
        return outputs;
    }

    public MessageFields getFields() {
        return fields;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("Message{");
        sb.append("timestamp=").append(getTimestamp());
        sb.append(", message='").append(getMessage()).append('\'');
        sb.append(", source='").append(getSource()).append('\'');
        sb.append(", input='").append(getInput()).append('\'');
        sb.append(", outputs=").append(Joiner.on(",").join(getOutputs()));
        sb.append(", fields=").append(getFields().asMap());
        sb.append('}');
        return sb.toString();
    }
}
