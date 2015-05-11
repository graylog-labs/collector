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
package org.graylog.collector.heartbeat;

import javax.inject.Inject;
import javax.inject.Provider;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class AgentRegistrationRequestProvider implements Provider<AgentRegistrationRequest> {
    private final String operatingSystem;
    private final String hostname;

    @Inject
    public AgentRegistrationRequestProvider() throws UnknownHostException {
        this.operatingSystem = System.getProperty("os.name", "unknown");
        this.hostname = InetAddress.getLocalHost().getHostName();
    }

    @Override
    public AgentRegistrationRequest get() {
        return AgentRegistrationRequest.create(hostname, AgentNodeDetailsSummary.create(operatingSystem));
    }
}
