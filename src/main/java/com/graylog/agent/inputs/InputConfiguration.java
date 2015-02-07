package com.graylog.agent.inputs;

import com.graylog.agent.ConfigurationError;

import java.util.List;

public interface InputConfiguration {
    List<ConfigurationError> validate();
}
