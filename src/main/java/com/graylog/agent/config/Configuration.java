package com.graylog.agent.config;

import java.util.Map;

public interface Configuration {
    String getId();

    Map<String, String> toStringValues();
}
