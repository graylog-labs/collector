package com.graylog.agent.cli.commands;

import com.graylog.agent.AgentVersion;
import io.airlift.airline.Command;

@Command(name = "version", description = "Show version information on STDOUT")
public class Version implements AgentCommand {
    @Override
    public void run() {
        final AgentVersion v = AgentVersion.CURRENT;
        final String message = String.format("Graylog Agent v%s (commit=%s, timestamp=%s)",
                v.version(), v.commitIdShort(), v.timestamp());

        System.out.println(message);
    }

    @Override
    public void stop() {
        // nothing to stop
    }
}
