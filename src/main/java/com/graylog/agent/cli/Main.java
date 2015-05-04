/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.graylog.agent.cli;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.graylog.agent.cli.commands.AgentCommand;
import com.graylog.agent.cli.commands.AgentHelp;
import com.graylog.agent.cli.commands.Server;
import com.graylog.agent.cli.commands.Version;
import io.airlift.airline.Cli;
import io.airlift.airline.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static AgentCommand command = null;

    public static void main(String[] args) {
        final Injector injector = Guice.createInjector();

        final Cli.CliBuilder<AgentCommand> cliBuilder = Cli.<AgentCommand>builder("graylog-agent")
                .withDescription("Graylog agent")
                .withDefaultCommand(AgentHelp.class)
                .withCommand(AgentHelp.class)
                .withCommand(Version.class)
                .withCommand(Server.class);

        final Cli<AgentCommand> cli = cliBuilder.build();

        try {
            command = cli.parse(args);
            configureShutdownHook(command);
            command.run();
        } catch (ParseException e) {
            LOG.error(e.getMessage());
            LOG.error("Exit");
        }
    }

    public static void stop(String[] args) {
        if (command != null) {
            command.stop();
        }
    }

    private static void configureShutdownHook(final AgentCommand command) {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                command.stop();
            }
        }));
    }
}
