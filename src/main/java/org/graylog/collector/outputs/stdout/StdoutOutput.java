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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class StdoutOutput extends OutputService {
    private static final Logger LOG = LoggerFactory.getLogger(StdoutOutput.class);

    public interface Factory extends OutputService.Factory<StdoutOutput, StdoutOutputConfiguration> {
        StdoutOutput create(StdoutOutputConfiguration configuration);
    }

    private final StdoutOutputConfiguration configuration;

    private final CountDownLatch stopLatch = new CountDownLatch(1);

    @Inject
    public StdoutOutput(@Assisted StdoutOutputConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    protected void triggerShutdown() {
        stopLatch.countDown();
    }

    @Override
    protected void run() throws Exception {
        stopLatch.await();
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
