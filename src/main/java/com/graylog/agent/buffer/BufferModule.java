package com.graylog.agent.buffer;

import com.graylog.agent.guice.AgentModule;

public class BufferModule extends AgentModule {
    @Override
    protected void configure() {
        bind(MessageBufferConfiguration.class);
        bind(Buffer.class).to(MessageBuffer.class);

        registerService(BufferProcessor.class);
    }
}
