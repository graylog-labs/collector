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
package com.graylog.agent.metrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Reporter;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Slf4jReporter;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractIdleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

public class MetricService extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(MetricService.class);

    private final MetricServiceConfiguration configuration;
    private final MetricRegistry metricRegistry;

    private final ConcurrentMap<Class<? extends Reporter>, ScheduledReporter> reporter = Maps.newConcurrentMap();

    @Inject
    public MetricService(MetricServiceConfiguration configuration, MetricRegistry metricRegistry) {
        this.configuration = configuration;
        this.metricRegistry = metricRegistry;
    }

    @Override
    protected void startUp() throws Exception {
        if (configuration.isEnableLog()) {
            startLoggingReporter();
        }
    }

    @Override
    protected void shutDown() throws Exception {
        for (ScheduledReporter entry : reporter.values()) {
            LOG.debug("Stopping metrics reporter: {}", entry);
            entry.stop();
        }
    }

    private void startLoggingReporter() {
        final Slf4jReporter loggingReporter = Slf4jReporter.forRegistry(metricRegistry)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .convertRatesTo(TimeUnit.SECONDS)
                .outputTo(LOG)
                .build();

        // Start the logging reporter if there is none yet.
        if (reporter.putIfAbsent(Slf4jReporter.class, loggingReporter) == null) {
            LOG.debug("Starting metrics reporter: {}", loggingReporter);
            reporter.get(Slf4jReporter.class).start(configuration.getReportDuration().getMillis(), TimeUnit.MILLISECONDS);
        }
    }
}
