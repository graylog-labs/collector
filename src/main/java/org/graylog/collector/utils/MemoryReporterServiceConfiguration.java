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
