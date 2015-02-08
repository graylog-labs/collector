package com.graylog.agent.inputs;

import com.graylog.agent.guice.AgentModule;
import com.graylog.agent.inputs.file.FileInput;
import com.graylog.agent.inputs.file.FileInputConfiguration;

public class InputsModule extends AgentModule {
    @Override
    protected void configure() {
        registerInput(FileInput.class, FileInputConfiguration.class);
    }
}
