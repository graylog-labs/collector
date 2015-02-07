package com.graylog.agent.outputs;

import com.graylog.agent.utils.ConfigurationUtils;
import com.typesafe.config.Config;

public class StdoutOutputConfiguration extends OutputConfiguration {
    public StdoutOutputConfiguration(String id, Config output) {
        super(id, output);
    }

    @Override
    public String toString() {
        return ConfigurationUtils.toString(this);
    }
}
