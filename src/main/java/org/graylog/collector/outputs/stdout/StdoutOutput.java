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
package org.graylog.collector.outputs.stdout;

import com.google.inject.assistedinject.Assisted;
import org.graylog.collector.Message;
import org.graylog.collector.config.ConfigurationUtils;
import org.graylog.collector.outputs.OutputService;

import javax.inject.Inject;
import java.util.Set;

public class StdoutOutput extends OutputService {
    public interface Factory extends OutputService.Factory<StdoutOutput, StdoutOutputConfiguration> {
        StdoutOutput create(StdoutOutputConfiguration configuration);
    }

    private final StdoutOutputConfiguration configuration;

    @Inject
    public StdoutOutput(@Assisted StdoutOutputConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void doStart() {
        notifyStarted();
    }

    @Override
    protected void doStop() {
        notifyStopped();
    }

    @Override
    public void write(Message message) {
        System.out.println("MESSAGE: " + message);
    }

    @Override
    public String toString() {
        return ConfigurationUtils.toString(configuration, this);
    }

    @Override
    public String getId() {
        return configuration.getId();
    }

    @Override
    public Set<String> getInputs() {
        return configuration.getInputs();
    }
}
