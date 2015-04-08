package com.graylog.agent.heartbeat;

import javax.inject.Provider;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class AgentRegistrationRequestProvider implements Provider<AgentRegistrationRequest> {
    private final String operatingSystem;
    private final String hostname;
    private final String agentId;

    public AgentRegistrationRequestProvider() throws UnknownHostException {
        this.operatingSystem = System.getProperty("os.name", "unknown");
        this.hostname = InetAddress.getLocalHost().getHostName();
        this.agentId = "veryUniqueAgentId";
    }

    @Override
    public AgentRegistrationRequest get() {
        return AgentRegistrationRequest.create(agentId, hostname, AgentNodeDetailsSummary.create(operatingSystem));
    }
}
