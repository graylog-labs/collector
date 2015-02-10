package com.graylog.agent.metrics;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Scopes;
import com.graylog.agent.guice.AgentModule;

public class MetricsModule extends AgentModule {
    @Override
    protected void configure() {
        bind(MetricRegistry.class).in(Scopes.SINGLETON);
    }
}
