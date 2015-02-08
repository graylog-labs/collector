package com.graylog.agent.outputs;

import com.graylog.agent.config.Configuration;

import java.util.Set;

public interface Output {
    String getId();

    Set<String> getInputs();

    public interface Factory<T extends Output, C extends Configuration> {
        T create(C configuration);
    }
}
