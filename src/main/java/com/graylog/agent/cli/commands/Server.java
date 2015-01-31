package com.graylog.agent.cli.commands;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;
import io.airlift.airline.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Command(name = "server", description = "Start the agent")
public class Server implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);

    @Override
    public void run() {
        LOG.info("Running {}", getClass().getCanonicalName());

        final Config config = ConfigFactory.parseFile(new File("config/agent.conf"));

        LOG.info("Config {}", config);

        Config files = config.getConfig("files");

        for (Map.Entry<String, ConfigValue> entry : files.root().entrySet()) {
            final Config file = ((ConfigObject) entry.getValue()).toConfig();

            LOG.info("file {}", entry.getKey());

            LOG.info("  path {}", file.getString("path"));

            if (file.hasPath("interval")) {
                LOG.info("  duration {}", file.getDuration("interval", TimeUnit.MINUTES));
            }
        }
    }
}
