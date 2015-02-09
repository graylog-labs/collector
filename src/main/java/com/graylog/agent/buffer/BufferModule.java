package com.graylog.agent.buffer;

import com.graylog.agent.guice.AgentModule;

public class BufferModule extends AgentModule {
    @Override
    protected void configure() {
        bind(Buffer.class).toInstance(new MessageBuffer(100));

        registerService(BufferProcessor.class);
    }
}
