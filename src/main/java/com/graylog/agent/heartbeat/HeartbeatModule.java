package com.graylog.agent.heartbeat;

import com.graylog.agent.guice.AgentModule;

public class HeartbeatModule extends AgentModule {
    @Override
    protected void configure() {
        bind(AgentRegistrationService.class).toProvider(AgentRegistrationServiceProvider.class);
        bind(AgentRegistrationRequest.class).toProvider(AgentRegistrationRequestProvider.class);
        registerService(HeartbeatService.class);
    }
}
