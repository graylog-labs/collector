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
package org.graylog.collector.cli.commands;

import com.google.common.util.concurrent.Service;
import com.google.inject.Injector;
import org.graylog.collector.AgentVersion;
import org.graylog.collector.buffer.BufferModule;
import org.graylog.collector.config.ConfigurationError;
import org.graylog.collector.config.ConfigurationModule;
import org.graylog.collector.config.ConfigurationRegistry;
import org.graylog.collector.guice.AgentInjector;
import org.graylog.collector.heartbeat.HeartbeatModule;
import org.graylog.collector.inputs.InputsModule;
import org.graylog.collector.metrics.MetricsModule;
import org.graylog.collector.outputs.OutputsModule;
import org.graylog.collector.serverapi.ServerApiModule;
import org.graylog.collector.services.AgentServiceManager;
import org.graylog.collector.services.ServicesModule;
import org.graylog.collector.utils.AgentIdModule;
import io.airlift.airline.Command;
import io.airlift.airline.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;

@Command(name = "server", description = "Start the agent")
public class Server implements AgentCommand {
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    @Option(name = "-f", description = "Path to configuration file.", required = true)
    private final File configFile = null;

    private AgentServiceManager serviceManager;

    @Override
    public void run() {
        LOG.info("Starting Agent v{} (commit {})", AgentVersion.CURRENT.version(), AgentVersion.CURRENT.commitIdShort());

        final Injector injector = getInjector();
        serviceManager = injector.getInstance(AgentServiceManager.class);

        validateConfiguration(serviceManager.getConfiguration());

        serviceManager.start();

        for (Map.Entry<Service.State, Service> entry : serviceManager.servicesByState().entries()) {
            LOG.info("Service {}: {}", entry.getKey().toString(), entry.getValue().toString());
        }

        serviceManager.awaitStopped();
    }

    @Override
    public void stop() {
        LOG.info("Stopping...");
        if (serviceManager != null) {
            serviceManager.stop();
        }
    }

    private Injector getInjector() {
        Injector injector = null;

        try {
            injector = AgentInjector.createInjector(new ConfigurationModule(configFile),
                    new BufferModule(),
                    new InputsModule(),
                    new OutputsModule(),
                    new ServicesModule(),
                    new MetricsModule(),
                    new ServerApiModule(),
                    new HeartbeatModule(),
                    new AgentIdModule());
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
