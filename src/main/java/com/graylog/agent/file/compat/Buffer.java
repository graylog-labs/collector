package com.graylog.agent.file.compat;

public interface Buffer {
    void insert(Message message, MessageInput input);
}
