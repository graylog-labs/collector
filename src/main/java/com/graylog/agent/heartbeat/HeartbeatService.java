package com.graylog.agent.heartbeat;

import com.google.common.util.concurrent.AbstractScheduledService;
import com.graylog.agent.utils.AgentId;
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
