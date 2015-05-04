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
package com.graylog.agent.inputs.eventlog;

import com.google.inject.assistedinject.Assisted;
import com.graylog.agent.config.ConfigurationUtils;
import com.graylog.agent.inputs.InputConfiguration;
import com.graylog.agent.inputs.InputService;
import com.typesafe.config.Config;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class WindowsEventlogInputConfiguration extends InputConfiguration {

    public interface Factory extends InputConfiguration.Factory<WindowsEventlogInputConfiguration> {
        @Override
        WindowsEventlogInputConfiguration create(String id, Config config);
    }

    @NotNull
    private final String sourceName;

    private final long pollInterval;

    private WindowsEventlogInput.Factory inputFactory;

    @Inject
    public WindowsEventlogInputConfiguration(@Assisted String id,
                                             @Assisted Config config,
                                             WindowsEventlogInput.Factory inputFactory) {
        super(id, config);
        this.inputFactory = inputFactory;

        if (config.hasPath("source-name")) {
            this.sourceName = config.getString("source-name");
        } else {
            this.sourceName = "Application";
        }

        if (config.hasPath("poll-interval")) {
            this.pollInterval = config.getDuration("poll-interval", TimeUnit.MILLISECONDS);
        } else {
            this.pollInterval = 1000L;
        }
    }

    public String getSourceName() {
        return sourceName;
    }

    public long getPollInterval() {
        return pollInterval;
    }

    @Override
    public InputService createInput() {
        return inputFactory.create(this);
    }

    @Override
    public Map<String, String> toStringValues() {
        return Collections.unmodifiableMap(new HashMap<String, String>(super.toStringValues()) {
            {
                put("sourceName", getSourceName());
                put("pollInterval", String.valueOf(getPollInterval()));
            }
        });
    }

    @Override
    public String toString() {
        return ConfigurationUtils.toString(this);
    }
}
