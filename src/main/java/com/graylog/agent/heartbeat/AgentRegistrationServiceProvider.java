package com.graylog.agent.heartbeat;

import retrofit.RestAdapter;

import javax.inject.Inject;
import javax.inject.Provider;

public class AgentRegistrationServiceProvider implements Provider<AgentRegistrationService> {
    private final RestAdapter restAdapter;

    @Inject
    public AgentRegistrationServiceProvider(RestAdapter restAdapter) {
        this.restAdapter = restAdapter;
    }

    @Override
    public AgentRegistrationService get() {
        return this.restAdapter.create(AgentRegistrationService.class);
    }
}
