package com.graylog.agent.file.compat;

import java.util.Map;

public class Configuration {
    private final Map<String, Object> values;

    public Configuration(Map<String, Object> values) {
        this.values = values;
    }

    public String getString(String key) {
        return (String) values.get(key);
    }
}
