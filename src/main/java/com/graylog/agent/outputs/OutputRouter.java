package com.graylog.agent.outputs;

import com.graylog.agent.buffer.BufferConsumer;
import com.graylog.agent.Message;
import com.graylog.agent.config.ConfigurationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Set;

public class OutputRouter implements BufferConsumer {
    private static final Logger LOG = LoggerFactory.getLogger(OutputRouter.class);
    private final Set<Output> outputs;

    @Inject
    public OutputRouter(ConfigurationRegistry configuration) {
        this.outputs = configuration.getOutputs();
    }

    @Override
    public void process(Message message) {
        LOG.info("Routing message to outputs. {}", message);

        for (Output output : outputs) {
            if (output.getInputs().contains(message.getInput()) || message.getOutputs().contains(output.getId())) {
                output.write(message);
            }
        }
    }

    @Override
    public String toString() {
        return "OutputRouter{}";
    }
}
