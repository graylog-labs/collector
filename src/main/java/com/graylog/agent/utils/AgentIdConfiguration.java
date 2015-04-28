package com.graylog.agent.utils;

import com.typesafe.config.Config;

import javax.inject.Inject;

public class AgentIdConfiguration {
    private static final String agentIdStatement = "agent-id";
    private final String agentId;

    @Inject
    public AgentIdConfiguration(Config config) {
        if (config.hasPath(agentIdStatement)) {
            this.agentId = config.getString(agentIdStatement);
        } else {
            this.agentId = "file:config/agent-id";
        }
    }

    public String getAgentId() {
        return agentId;
    }
}

