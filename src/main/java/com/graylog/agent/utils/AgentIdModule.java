package com.graylog.agent.utils;

import com.graylog.agent.guice.AgentModule;

public class AgentIdModule extends AgentModule {
    @Override
    protected void configure() {
        bind(AgentIdConfiguration.class);
        bind(AgentId.class);
    }
}
