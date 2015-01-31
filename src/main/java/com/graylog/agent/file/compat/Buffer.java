package com.graylog.agent.file.compat;

public interface Buffer {
    void insertCached(Message message, MessageInput input);
}
