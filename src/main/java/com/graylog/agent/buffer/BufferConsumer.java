package com.graylog.agent.buffer;

import com.graylog.agent.Message;

public interface BufferConsumer {
    void process(Message message);
}
