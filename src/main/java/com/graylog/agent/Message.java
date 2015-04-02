package com.graylog.agent;

import com.google.common.base.Joiner;
import org.joda.time.DateTime;

import java.util.Set;

public class Message {
    public enum Level {
        EMERGENCY, ALERT, CRITICAL, ERROR, WARNING, NOTICE, INFO, DEBUG;
    }

    private final String message;
    private final String source;
    private final DateTime timestamp;
    private final String input;
    private final Set<String> outputs;
    private final MessageFields fields;
    private final Level level;

    public Message(String message, String source, DateTime timestamp, Level level, String input, Set<String> outputs) {
        this(message, source, timestamp, level, input, outputs, new MessageFields());
    }

    public Message(String message, String source, DateTime timestamp, Level level, String input, Set<String> outputs, MessageFields fields) {
        this.source = source;
        this.message = message;
        this.timestamp = timestamp;
        this.level = level;
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

    public Level getLevel() {
        return level;
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
        sb.append(", level='").append(getLevel().toString()).append('\'');
        sb.append(", message='").append(getMessage()).append('\'');
        sb.append(", source='").append(getSource()).append('\'');
        sb.append(", input='").append(getInput()).append('\'');
        sb.append(", outputs=").append(Joiner.on(",").join(getOutputs()));
        sb.append(", fields=").append(getFields().asMap());
        sb.append('}');
        return sb.toString();
    }
}
