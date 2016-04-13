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

import com.google.common.base.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.UnknownHostException;

import static java.util.Objects.requireNonNull;

public class CollectorHostNameSupplier implements Supplier<String> {
    private static final Logger LOG = LoggerFactory.getLogger(CollectorHostNameSupplier.class);

    private final String defaultHostName;
    private final CollectorId collectorId;

    public CollectorHostNameSupplier(@Nullable String defaultHostName, CollectorId collectorId) {
        this.defaultHostName = defaultHostName;
        this.collectorId = requireNonNull(collectorId);
    }

    @Override
    public String get() {
        return defaultHostName == null ? detectHostname() : defaultHostName;
    }

    private String detectHostname() {
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = System.getenv("HOSTNAME");
            if (hostname == null) {
                hostname = System.getenv("COMPUTERNAME");
            }
            if (hostname == null) {
                hostname = "unknown-" + collectorId.toString();
                LOG.warn("Unable to detect the local host name, falling back to \"{}\". "
                        + "Use the \"host-name\" configuration setting to override.", hostname);
            }
        }

        return hostname;
    }
}
