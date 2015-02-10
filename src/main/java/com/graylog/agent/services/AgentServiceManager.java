package com.graylog.agent.services;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.graylog.agent.config.ConfigurationRegistry;

import javax.inject.Inject;

public class AgentServiceManager {
    private final ServiceManager serviceManager;
    private final ConfigurationRegistry configuration;

    @Inject
    public AgentServiceManager(ServiceManager serviceManager, ConfigurationRegistry configuration) {
        this.serviceManager = serviceManager;
        this.configuration = configuration;
    }

    public ConfigurationRegistry getConfiguration() {
        return configuration;
    }

    public void start() {
        serviceManager.startAsync().awaitHealthy();
    }

    public void stop() {
        serviceManager.stopAsync().awaitStopped();
    }

    public void awaitStopped() {
        serviceManager.awaitStopped();
    }

    public ImmutableMultimap<Service.State, Service> servicesByState() {
        return serviceManager.servicesByState();
    }
}
