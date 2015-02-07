package com.graylog.agent.utils;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.graylog.agent.config.Configuration;

import javax.annotation.Nullable;
import java.util.Map;

public class ConfigurationUtils {
    public static String toString(Configuration configurationObject) {
        return toString(configurationObject, configurationObject);
    }

    public static String toString(Configuration configurationObject, Object nameClass) {
        final Map<String, String> values = configurationObject.toStringValues();
        final Iterable<String> strings = Iterables.transform(values.entrySet(), new Function<Map.Entry<String, String>, String>() {
            @Nullable
            @Override
            public String apply(@Nullable Map.Entry<String, String> input) {
                if (input == null) {
                    return "";
                }
                return String.format("%s='%s'", input.getKey(), input.getValue());
            }
        });

        final StringBuffer sb = new StringBuffer(nameClass.getClass().getSimpleName());
        sb.append('{');
        sb.append(Joiner.on(", ").join(strings));
        sb.append('}');

        return sb.toString();
    }
}
