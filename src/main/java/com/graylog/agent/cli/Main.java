package com.graylog.agent.cli;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.graylog.agent.cli.commands.Server;
import com.graylog.agent.cli.commands.Version;
import io.airlift.airline.Cli;
import io.airlift.airline.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    private static final Logger LOG = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        final Injector injector = Guice.createInjector();

        final Cli.CliBuilder<Runnable> cliBuilder = Cli.<Runnable>builder("graylog-agent")
                .withDescription("Graylog agent")
                .withCommand(Version.class)
                .withCommand(Server.class);

        final Cli<Runnable> cli = cliBuilder.build();

        try {
            cli.parse(args).run();
        } catch (ParseException e) {
            LOG.error(e.getMessage());
            LOG.error("Exit");
        }
    }
}
