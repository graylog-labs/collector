package com.graylog.agent.heartbeat;

import javax.inject.Inject;
import javax.inject.Provider;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class AgentRegistrationRequestProvider implements Provider<AgentRegistrationRequest> {
    private final String operatingSystem;
    private final String hostname;

    @Inject
    public AgentRegistrationRequestProvider() throws UnknownHostException {
        this.operatingSystem = System.getProperty("os.name", "unknown");
        this.hostname = InetAddress.getLocalHost().getHostName();
    }

    @Override
    public AgentRegistrationRequest get() {
        return AgentRegistrationRequest.create(hostname, AgentNodeDetailsSummary.create(operatingSystem));
    }
}
