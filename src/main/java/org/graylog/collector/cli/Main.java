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
package org.graylog.collector.cli;

import io.airlift.airline.Cli;
import io.airlift.airline.ParseException;
import org.graylog.collector.cli.commands.CollectorCommand;
import org.graylog.collector.cli.commands.CollectorHelp;
import org.graylog.collector.cli.commands.Run;
import org.graylog.collector.cli.commands.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    private static CollectorCommand command = null;

    public static void main(String[] args) {
        final Cli.CliBuilder<CollectorCommand> cliBuilder = Cli.<CollectorCommand>builder("graylog-collector")
                .withDescription("Graylog Collector")
                .withDefaultCommand(CollectorHelp.class)
                .withCommand(CollectorHelp.class)
                .withCommand(Version.class)
                .withCommand(Run.class);

        final Cli<CollectorCommand> cli = cliBuilder.build();

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

    private static void configureShutdownHook(final CollectorCommand command) {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                command.stop();
            }
        }));
    }
}
