package com.graylog.agent.buffer;

import com.graylog.agent.file.compat.Message;

public interface BufferConsumer {
    void process(Message message);
}
