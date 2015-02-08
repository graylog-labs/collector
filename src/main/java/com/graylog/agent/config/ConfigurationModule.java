package com.graylog.agent.config;

import com.graylog.agent.guice.AgentModule;
import com.typesafe.config.Config;

import java.io.File;

public class ConfigurationModule extends AgentModule {
    private final Config config;

    public ConfigurationModule(File configFile) {
        this.config = ConfigurationParser.parse(configFile);
    }

    @Override
    protected void configure() {
        bind(Config.class).toInstance(config);
    }
}
