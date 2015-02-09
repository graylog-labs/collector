package com.graylog.agent.services;

import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Provider;
import com.graylog.agent.config.ConfigurationRegistry;

import javax.inject.Inject;
import java.util.Set;

public class ServiceManagerProvider implements Provider<ServiceManager> {
    private final Set<Service> services;
    private final ConfigurationRegistry configuration;

    @Inject
    public ServiceManagerProvider(Set<Service> services, ConfigurationRegistry configuration) {
        this.services = services;
        this.configuration = configuration;
    }

    @Override
    public ServiceManager get() {
        final ImmutableSet<Service> allServices = ImmutableSet.<Service>builder()
                .addAll(services)
                .addAll(configuration.getServices())
                .build();

        return new ServiceManager(allServices);
    }
}
