package com.graylog.agent.heartbeat;

import com.google.common.util.concurrent.AbstractScheduledService;
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

    private final AgentRegistrationService agentRegistrationService;
    private final AgentRegistrationRequest agentRegistrationRequest;
    private final Config config;

    @Inject
    public HeartbeatService(AgentRegistrationService agentRegistrationService,
                            AgentRegistrationRequest agentRegistrationRequest,
                            Config config) {
        this.agentRegistrationService = agentRegistrationService;
        this.agentRegistrationRequest = agentRegistrationRequest;
        this.config = config;
    }

    @Override
    protected void runOneIteration() throws Exception {
        try {
            agentRegistrationService.register(agentRegistrationRequest);
        } catch (RetrofitError e) {
            final Response response = e.getResponse();
            LOG.warn("Unable to send successfull heartbeat to Graylog server, result was: {} - {}", response.getStatus(), response.getReason());
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
