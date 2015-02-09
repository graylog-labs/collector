package com.graylog.agent.services;

import com.google.common.util.concurrent.ServiceManager;
import com.graylog.agent.guice.AgentModule;

public class ServicesModule extends AgentModule {
    @Override
    protected void configure() {
        bind(AgentServiceManager.class);
        bind(ServiceManager.class).toProvider(ServiceManagerProvider.class);
    }
}
