package com.graylog.agent;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Map;

public class MessageFields {
    private final Map<String, Integer> intFields = Maps.newConcurrentMap();
    private final Map<String, Long> longFields = Maps.newConcurrentMap();
    private final Map<String, Boolean> booleanFields = Maps.newConcurrentMap();
    private final Map<String, String> stringFields = Maps.newConcurrentMap();

    public void put(String key, int value) {
        intFields.put(key, value);
    }

    public void put(String key, long value) {
        longFields.put(key, value);
    }

    public void put(String key, boolean value) {
        booleanFields.put(key, value);
    }

    public void put(String key, String value) {
        stringFields.put(key, value);
    }

    public Map<String, Object> asMap() {
        return ImmutableMap.<String, Object>builder()
                .putAll(intFields)
                .putAll(longFields)
                .putAll(booleanFields)
                .putAll(stringFields)
                .build();
    }
}
