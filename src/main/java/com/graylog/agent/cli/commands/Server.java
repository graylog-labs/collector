package com.graylog.agent.cli.commands;

import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.graylog.agent.buffer.BufferModule;
import com.graylog.agent.config.ConfigurationError;
import com.graylog.agent.buffer.BufferConsumer;
import com.graylog.agent.buffer.BufferProcessor;
import com.graylog.agent.buffer.MessageBuffer;
import com.graylog.agent.config.ConfigurationModule;
import com.graylog.agent.config.ConfigurationProcessor;
import com.graylog.agent.inputs.InputsModule;
import com.graylog.agent.outputs.OutputRouter;
import com.graylog.agent.outputs.OutputsModule;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Command(name = "server", description = "Start the agent")
public class Server implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    @Option(name = "-f", description = "Path to configuration file.", required = true)
    private final File configFile = null;

    @Override
    public void run() {
        LOG.info("Running {}", getClass().getCanonicalName());

        final Injector injector = Guice.createInjector(new ConfigurationModule(configFile),
                new BufferModule(),
                new InputsModule(),
                new OutputsModule());

        final MessageBuffer buffer = new MessageBuffer(100);
        final ConfigurationProcessor configuration = injector.getInstance(ConfigurationProcessor.class);

        validateConfiguration(configuration);

        final Set<Service> services = Sets.newHashSet();
        final HashSet<BufferConsumer> consumers = Sets.<BufferConsumer>newHashSet(new OutputRouter());

        services.add(new BufferProcessor(buffer, consumers));
        services.addAll(configuration.getServices());

        final ServiceManager serviceManager = new ServiceManager(services);

        serviceManager.startAsync().awaitHealthy();

        for (Map.Entry<Service.State, Service> entry : serviceManager.servicesByState().entries()) {
            LOG.info("Service {}: {}", entry.getKey().toString(), entry.getValue().toString());
        }

        serviceManager.awaitStopped();
    }

    private void validateConfiguration(ConfigurationProcessor configurationProcessor) {
        if (!configurationProcessor.isValid()) {
            for (ConfigurationError error : configurationProcessor.getErrors()) {
                LOG.error("Configuration Error: {}", error.getMesssage());
            }

            LOG.info("Exit");
            System.exit(1);
        }
    }
}
