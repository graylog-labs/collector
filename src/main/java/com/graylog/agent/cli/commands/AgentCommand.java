package com.graylog.agent.cli.commands;

public interface AgentCommand extends Runnable {
    void stop();
}
