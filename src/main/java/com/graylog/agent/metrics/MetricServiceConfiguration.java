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

import com.typesafe.config.Config;
import org.joda.time.Duration;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class MetricServiceConfiguration {
    private boolean enableLog = false;
    private Duration reportDuration = new Duration(60000);

    @Inject
    public MetricServiceConfiguration(Config config) {
        if (config.hasPath("metrics")) {
            final Config metrics = config.getConfig("metrics");

            this.enableLog = metrics.hasPath("enable-logging") && metrics.getBoolean("enable-logging");

            if (metrics.hasPath("log-duration")) {
                this.reportDuration = new Duration(metrics.getDuration("log-duration", TimeUnit.MILLISECONDS));
            }
        }
    }

    public boolean isEnableLog() {
        return enableLog;
    }

    public Duration getReportDuration() {
        return reportDuration;
    }
}
