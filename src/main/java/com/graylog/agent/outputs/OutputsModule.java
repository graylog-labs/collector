package com.graylog.agent.outputs;

import com.graylog.agent.guice.AgentModule;
import com.graylog.agent.outputs.gelf.GelfOutput;
import com.graylog.agent.outputs.gelf.GelfOutputConfiguration;
import com.graylog.agent.outputs.stdout.StdoutOutput;
import com.graylog.agent.outputs.stdout.StdoutOutputConfiguration;

public class OutputsModule extends AgentModule {
    @Override
    protected void configure() {
        registerOutput(GelfOutput.class, GelfOutputConfiguration.class);
        registerOutput(StdoutOutput.class, StdoutOutputConfiguration.class);

        registerBufferConsumer(OutputRouter.class);
    }
}
