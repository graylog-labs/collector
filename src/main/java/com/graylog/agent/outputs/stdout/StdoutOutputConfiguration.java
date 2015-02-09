package com.graylog.agent.outputs.stdout;

import com.google.inject.assistedinject.Assisted;
import com.graylog.agent.annotations.AgentConfigurationFactory;
import com.graylog.agent.annotations.AgentOutputConfiguration;
import com.graylog.agent.config.ConfigurationUtils;
import com.graylog.agent.outputs.OutputConfiguration;
import com.typesafe.config.Config;

import javax.inject.Inject;

@AgentOutputConfiguration(type = "stdout")
public class StdoutOutputConfiguration extends OutputConfiguration {
    private final StdoutOutput.Factory outputFactory;

    @AgentConfigurationFactory
    public interface Factory extends OutputConfiguration.Factory<StdoutOutputConfiguration> {
        @Override
        StdoutOutputConfiguration create(String id, Config config);
    }

    @Inject
    public StdoutOutputConfiguration(@Assisted String id,
                                     @Assisted Config output,
                                     StdoutOutput.Factory outputFactory) {
        super(id, output);
        this.outputFactory = outputFactory;
    }

    @Override
    public StdoutOutput createOutput() {
        return outputFactory.create(this);
    }

    @Override
    public String toString() {
        return ConfigurationUtils.toString(this);
    }
}
