package com.graylog.agent.outputs;

import com.graylog.agent.config.Configuration;
import com.typesafe.config.Config;

public class StdoutOutputConfiguration implements Configuration {
    private final String id;

    public StdoutOutputConfiguration(String id, Config output) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "StdoutOutputConfiguration{" +
                "id='" + id + '\'' +
                '}';
    }
}
