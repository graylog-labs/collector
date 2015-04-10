package com.graylog.agent.heartbeat;

import com.graylog.agent.utils.AgentId;

import javax.inject.Inject;
import javax.inject.Provider;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class AgentRegistrationRequestProvider implements Provider<AgentRegistrationRequest> {
    private final String operatingSystem;
    private final String hostname;
    private final String agentId;

    @Inject
    public AgentRegistrationRequestProvider(AgentId agentId) throws UnknownHostException {
        this.operatingSystem = System.getProperty("os.name", "unknown");
        this.hostname = InetAddress.getLocalHost().getHostName();
        this.agentId = agentId.toString();
    }

    @Override
    public AgentRegistrationRequest get() {
        return AgentRegistrationRequest.create(agentId, hostname, AgentNodeDetailsSummary.create(operatingSystem));
    }
}
