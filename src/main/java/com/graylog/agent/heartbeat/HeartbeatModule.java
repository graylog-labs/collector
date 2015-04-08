package com.graylog.agent.heartbeat;

import com.graylog.agent.annotations.GraylogServerURL;
import com.graylog.agent.guice.AgentModule;

public class HeartbeatModule extends AgentModule {
    @Override
    protected void configure() {
        bind(String.class).annotatedWith(GraylogServerURL.class).toInstance("http://localhost:12900");
        bind(AgentRegistrationService.class).toProvider(AgentRegistrationServiceProvider.class);
        bind(AgentRegistrationRequest.class).toProvider(AgentRegistrationRequestProvider.class);
        registerService(HeartbeatService.class);
    }
}
