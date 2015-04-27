package com.graylog.agent.cli.commands;

import io.airlift.airline.Help;

public class AgentHelp extends Help implements AgentCommand {
    @Override
    public void stop() {
        // nothing to stop
    }
}
