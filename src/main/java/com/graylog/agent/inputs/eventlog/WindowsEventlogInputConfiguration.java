package com.graylog.agent.inputs.eventlog;

import com.google.inject.assistedinject.Assisted;
import com.graylog.agent.annotations.AgentConfigurationFactory;
import com.graylog.agent.annotations.AgentInputConfiguration;
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

@AgentInputConfiguration(type = "windows-eventlog")
public class WindowsEventlogInputConfiguration extends InputConfiguration {

    @AgentConfigurationFactory
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
