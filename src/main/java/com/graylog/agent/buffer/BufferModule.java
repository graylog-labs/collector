package com.graylog.agent.buffer;

import com.google.inject.Scopes;
import com.graylog.agent.guice.AgentModule;

public class BufferModule extends AgentModule {
    @Override
    protected void configure() {
        bind(MessageBufferConfiguration.class);
        bind(Buffer.class).to(MessageBuffer.class).in(Scopes.SINGLETON);

        registerService(BufferProcessor.class);
    }
}
