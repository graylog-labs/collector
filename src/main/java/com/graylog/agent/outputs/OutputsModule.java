package com.graylog.agent.outputs;

import com.graylog.agent.guice.AgentModule;

public class OutputsModule extends AgentModule {
    @Override
    protected void configure() {
        registerOutputConfiguration(GelfOutputConfiguration.class);
        registerOutput(GelfOutput.class);

        registerOutputConfiguration(StdoutOutputConfiguration.class);
        registerOutput(StdoutOutput.class);
    }
}
