package com.graylog.agent.file.compat;

import org.joda.time.DateTime;

public class Message {
    private final DateTime timestamp;
    private String message;
    private String source;

    public Message(String message, String source, DateTime timestamp) {
        this.source = source;
        this.message = message;

        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public String getSource() {
        return source;
    }
}
