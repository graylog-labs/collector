package com.graylog.agent.buffer;

import com.graylog.agent.file.compat.Message;
import com.graylog.agent.inputs.Input;

public interface Buffer {
    void insert(Message message, Input input);

    Message remove();
}
