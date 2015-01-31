package com.graylog.agent.file.compat;

public abstract class Buffer {
    public abstract void insertFailFast(Message message, MessageInput sourceInput);

    public void insertCached(Message message, MessageInput input) {

    }
}
