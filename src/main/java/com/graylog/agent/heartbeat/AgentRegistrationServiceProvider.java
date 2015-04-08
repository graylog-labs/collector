package com.graylog.agent.heartbeat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graylog.agent.annotations.GraylogServerURL;
import retrofit.RestAdapter;
import retrofit.converter.JacksonConverter;

import javax.inject.Inject;
import javax.inject.Provider;

public class AgentRegistrationServiceProvider implements Provider<AgentRegistrationService> {
    private final RestAdapter restAdapter;

    @Inject
    public AgentRegistrationServiceProvider(@GraylogServerURL String graylogServerURL) {
        this.restAdapter = new RestAdapter.Builder()
                .setEndpoint(graylogServerURL)
                .setConverter(new JacksonConverter(new ObjectMapper()))
                .build();
    }

    @Override
    public AgentRegistrationService get() {
        return this.restAdapter.create(AgentRegistrationService.class);
    }
}
