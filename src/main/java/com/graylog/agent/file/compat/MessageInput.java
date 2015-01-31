package com.graylog.agent.file.compat;

public interface MessageInput {
    void initialize(Configuration config);

    void launch(Buffer mockBuffer);
}
