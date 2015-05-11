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

import com.google.common.util.concurrent.AbstractScheduledService;
import org.graylog.collector.utils.AgentId;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit.RetrofitError;
import retrofit.client.Response;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class HeartbeatService extends AbstractScheduledService {
    private static final Logger LOG = LoggerFactory.getLogger(HeartbeatService.class);
    private static final String heartbeatIntervalParameter = "heartbeat-interval";
    private static final int defaultHeartbeatInterval = 5;
    private static final String enableRegistrationParameter = "enable-registration";

    private final AgentRegistrationService agentRegistrationService;
    private final AgentRegistrationRequest agentRegistrationRequest;
    private final Config config;
    private final String agentId;

    @Inject
    public HeartbeatService(AgentRegistrationService agentRegistrationService,
                            AgentRegistrationRequest agentRegistrationRequest,
                            Config config,
                            AgentId agentId) {
        this.agentRegistrationService = agentRegistrationService;
        this.agentRegistrationRequest = agentRegistrationRequest;
        this.config = config;
        this.agentId = agentId.toString();
    }

    @Override
    protected void runOneIteration() throws Exception {
        if (config.hasPath(enableRegistrationParameter) && !config.getBoolean(enableRegistrationParameter)) {
            return;
        }
        try {
            agentRegistrationService.register(this.agentId, this.agentRegistrationRequest);
        } catch (RetrofitError e) {
            final Response response = e.getResponse();
            if (response != null)
                LOG.warn("Unable to send heartbeat to Graylog server, result was: {} - {}", response.getStatus(), response.getReason());
            else {
                final String message;
                if (e.getCause() != null)
                    message = e.getCause().getClass().getSimpleName() + ": " + e.getCause().getMessage();
                else
                    message = e.getClass().getSimpleName() + ": " + e.getMessage();
                LOG.warn("Unable to send heartbeat to Graylog server: {}", message);
            }
        }
    }

    @Override
    protected Scheduler scheduler() {
        return Scheduler.newFixedRateSchedule(0, heartbeatInterval(config), TimeUnit.SECONDS);
    }

    private int heartbeatInterval(Config config) {
        if (config.hasPath(heartbeatIntervalParameter)) {
            return config.getInt(heartbeatIntervalParameter);
        } else {
            return defaultHeartbeatInterval;
        }
    }
}
