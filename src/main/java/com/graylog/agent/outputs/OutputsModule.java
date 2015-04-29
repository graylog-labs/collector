package com.graylog.agent.outputs;

import com.graylog.agent.guice.AgentModule;
import com.graylog.agent.outputs.gelf.GelfOutput;
import com.graylog.agent.outputs.gelf.GelfOutputConfiguration;
import com.graylog.agent.outputs.stdout.StdoutOutput;
import com.graylog.agent.outputs.stdout.StdoutOutputConfiguration;

public class OutputsModule extends AgentModule {
    @Override
    protected void configure() {
        registerOutput("gelf",
                GelfOutput.class,
                GelfOutput.Factory.class,
                GelfOutputConfiguration.class,
                GelfOutputConfiguration.Factory.class);

        registerOutput("stdout",
                StdoutOutput.class,
                StdoutOutput.Factory.class,
                StdoutOutputConfiguration.class,
                StdoutOutputConfiguration.Factory.class);

        registerBufferConsumer(OutputRouter.class);
    }
}
