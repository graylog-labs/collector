package com.graylog.agent.serverapi;

import com.typesafe.config.Config;

import javax.inject.Inject;
import javax.inject.Provider;

public class ServerURLProvider implements Provider<String> {
    private static final String serverURLStatement = "server-url";
    private final Config config;

    @Inject
    public ServerURLProvider(Config config) {
        this.config = config;
    }

    @Override
    public String get() {
        if (config.hasPath(serverURLStatement)) {
            return config.getString(serverURLStatement);
        } else {
            return "http://localhost:12900";
        }
    }
}
