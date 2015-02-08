package com.graylog.agent.buffer;

import com.google.inject.AbstractModule;

public class BufferModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(Buffer.class).toInstance(new MessageBuffer(100));
    }
}
