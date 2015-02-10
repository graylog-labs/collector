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
