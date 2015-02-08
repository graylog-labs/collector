package com.graylog.agent.inputs;

import com.graylog.agent.guice.AgentModule;

public class InputsModule extends AgentModule {
    @Override
    protected void configure() {
        registerInputConfiguration(FileInputConfiguration.class);
        registerInput(FileInput.class);
    }
}
