package com.graylog.agent.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;

public class ConfigurationParser {
    public static class Error extends RuntimeException {
        public Error(String message) {
            super(message);
        }
    }

    public static Config parse(File configFile) {
        Config config = null;

        if (configFile.exists() && configFile.canRead()) {
            config = ConfigFactory.parseFile(configFile);

            if (config.isEmpty()) {
                throw new Error("Empty configuration!");
            }
        } else {
            throw new Error("Configuration file " + configFile + " does not exist or is not readable!");
        }

        return config;
    }
}
