package com.graylog.agent.heartbeat;

import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.PUT;
import retrofit.http.Path;

public interface AgentRegistrationService {
    @PUT("/system/agents/{agentId}")
    Response register(@Path("agentId") String agentId, @Body AgentRegistrationRequest request);
}
