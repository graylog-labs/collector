package com.graylog.agent.outputs;

import com.graylog.agent.buffer.BufferConsumer;
import com.graylog.agent.file.compat.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutputRouter implements BufferConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(OutputRouter.class);

    @Override
    public void process(Message message) {
        LOG.info("Routing message to outputs. {}", message);
    }

    @Override
    public String toString() {
        return "OutputRouter{}";
    }
}
