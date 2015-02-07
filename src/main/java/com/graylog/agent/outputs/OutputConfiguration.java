package com.graylog.agent.outputs;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.graylog.agent.config.Configuration;
import com.typesafe.config.Config;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class OutputConfiguration implements Configuration {
    @NotBlank
    private final String id;

    @NotNull
    private Set<String> inputs = Sets.newHashSet();

    public OutputConfiguration(String id, Config config) {
        this.id = id;

        if (config.hasPath("inputs")) {
            this.inputs = Sets.newHashSet(Splitter.on(",").omitEmptyStrings().trimResults().split(config.getString("inputs")));
        }
    }

    @Override
    public String getId() {
        return id;
    }

    public Set<String> getInputs() {
        return inputs;
    }

    @Override
    public Map<String, String> toStringValues() {
        return Collections.unmodifiableMap(new HashMap<String, String>() {
            {
                put("id", getId());
                put("inputs", Joiner.on(",").join(getInputs()));
            }
        });
    }
}
