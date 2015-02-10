package com.graylog.agent.cli.commands;

import com.google.common.util.concurrent.Service;
import com.google.inject.Injector;
import com.graylog.agent.buffer.BufferModule;
import com.graylog.agent.config.ConfigurationError;
import com.graylog.agent.config.ConfigurationModule;
import com.graylog.agent.config.ConfigurationRegistry;
import com.graylog.agent.guice.AgentInjector;
import com.graylog.agent.inputs.InputsModule;
import com.graylog.agent.metrics.MetricsModule;
import com.graylog.agent.outputs.OutputsModule;
import com.graylog.agent.services.AgentServiceManager;
import com.graylog.agent.services.ServicesModule;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

@Command(name = "server", description = "Start the agent")
public class Server implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    @Option(name = "-f", description = "Path to configuration file.", required = true)
    private final File configFile = null;

    @Override
    public void run() {
        LOG.info("Running {}", getClass().getCanonicalName());

        final Injector injector = getInjector();
        final AgentServiceManager serviceManager = injector.getInstance(AgentServiceManager.class);

        validateConfiguration(serviceManager.getConfiguration());

        serviceManager.start();

        for (Map.Entry<Service.State, Service> entry : serviceManager.servicesByState().entries()) {
            LOG.info("Service {}: {}", entry.getKey().toString(), entry.getValue().toString());
        }

        serviceManager.awaitStopped();
    }

    private Injector getInjector() {
        Injector injector = null;

        try {
            injector = AgentInjector.createInjector(new ConfigurationModule(configFile),
                    new BufferModule(),
                    new InputsModule(),
                    new OutputsModule(),
                    new ServicesModule(),
                    new MetricsModule());
        } catch (Exception e) {
            LOG.error("ERROR: {}", e.getMessage());
            LOG.debug("Detailed injection creation error", e);
            doExit();
        }

        return injector;
    }

    private void validateConfiguration(ConfigurationRegistry configurationRegistry) {
        if (!configurationRegistry.isValid()) {
            for (ConfigurationError error : configurationRegistry.getErrors()) {
                LOG.error("Configuration Error: {}", error.getMesssage());
            }

            doExit();
        }
    }

    private void doExit() {
        LOG.info("Exit");
        System.exit(1);
    }
}
