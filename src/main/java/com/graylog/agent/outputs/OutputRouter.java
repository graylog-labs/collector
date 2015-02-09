package com.graylog.agent.outputs;

import com.graylog.agent.Message;
import com.graylog.agent.buffer.BufferConsumer;
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
        LOG.debug("Routing message to outputs. {}", message);

        for (Output output : outputs) {
            final Set<String> outputInputs = output.getInputs();
            final Set<String> messageOutputs = message.getOutputs();

            if (outputInputs.isEmpty() && messageOutputs.isEmpty()) {
                output.write(message);
            } else if (outputInputs.contains(message.getInput()) || messageOutputs.contains(output.getId())) {
                output.write(message);
            }
        }
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("OutputRouter{");
        sb.append("outputs=").append(outputs);
        sb.append('}');
        return sb.toString();
    }
}
