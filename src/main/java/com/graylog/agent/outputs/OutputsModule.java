package com.graylog.agent.outputs;

import com.graylog.agent.guice.AgentModule;
import com.graylog.agent.outputs.gelf.GelfOutput;
import com.graylog.agent.outputs.gelf.GelfOutputConfiguration;
import com.graylog.agent.outputs.stdout.StdoutOutput;
import com.graylog.agent.outputs.stdout.StdoutOutputConfiguration;

public class OutputsModule extends AgentModule {
    @Override
    protected void configure() {
        registerOutputConfiguration(GelfOutputConfiguration.class);
        registerOutput(GelfOutput.class);

        registerOutputConfiguration(StdoutOutputConfiguration.class);
        registerOutput(StdoutOutput.class);
    }
}
