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
