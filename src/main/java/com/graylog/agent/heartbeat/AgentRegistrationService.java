package com.graylog.agent.heartbeat;

import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.POST;

public interface AgentRegistrationService {
    @POST("/system/agents/register")
    Response register(@Body AgentRegistrationRequest request);
}
