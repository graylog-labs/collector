package com.graylog.agent.buffer;

import com.graylog.agent.Message;

public interface Buffer {
    void insert(Message message);

    Message remove();
}
