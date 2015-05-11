/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.collector.outputs;

import org.graylog.collector.Message;
import org.graylog.collector.buffer.BufferConsumer;
import org.graylog.collector.config.ConfigurationRegistry;
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
