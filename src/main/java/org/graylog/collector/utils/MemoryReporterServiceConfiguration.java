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
package org.graylog.collector.utils;

import com.typesafe.config.Config;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class MemoryReporterServiceConfiguration {
    private long interval = 1000L;
    private boolean enable = false;

    @Inject
    public MemoryReporterServiceConfiguration(final Config config) {
        if (config.hasPath("debug")) {
            final Config debug = config.getConfig("debug");

            if (debug.hasPath("memory-reporter")) {
                this.enable = debug.getBoolean("memory-reporter");
            }

            if (debug.hasPath("memory-reporter-interval")) {
                this.interval = debug.getDuration("memory-reporter-interval", TimeUnit.MILLISECONDS);
            }
        }
    }

    public long getInterval() {
        return interval;
    }

    public boolean isEnable() {
        return enable;
    }
}
