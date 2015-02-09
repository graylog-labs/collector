package com.graylog.agent.file.splitters;

import java.util.Map;

public class ContentSplitterConfiguration {
    private final Map<String, Object> values;

    public ContentSplitterConfiguration(Map<String, Object> values) {
        this.values = values;
    }

    public String getString(String key) {
        return (String) values.get(key);
    }
}
