package com.graylog.agent.inputs;

import com.graylog.agent.guice.AgentModule;
import com.graylog.agent.inputs.eventlog.WindowsEventlogInput;
import com.graylog.agent.inputs.eventlog.WindowsEventlogInputConfiguration;
import com.graylog.agent.inputs.file.FileInput;
import com.graylog.agent.inputs.file.FileInputConfiguration;
import com.graylog.agent.utils.Utils;

public class InputsModule extends AgentModule {
    @Override
    protected void configure() {
        registerInput(FileInput.class, FileInputConfiguration.class);

        if (Utils.isWindows()) {
            registerInput(WindowsEventlogInput.class, WindowsEventlogInputConfiguration.class);
        }
    }
}
