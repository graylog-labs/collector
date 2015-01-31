package com.graylog.agent.cli;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.graylog.agent.cli.commands.Server;
import io.airlift.airline.Cli;

public class Main {
    public static void main(String[] args) {
        final Injector injector = Guice.createInjector();

        final Cli.CliBuilder<Runnable> cliBuilder = Cli.<Runnable>builder("graylog-agent")
                .withDescription("Graylog agent")
                .withCommand(Server.class);

        final Cli<Runnable> cli = cliBuilder.build();

        cli.parse(args).run();
    }
}
