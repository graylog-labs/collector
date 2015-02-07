package com.graylog.agent.inputs;

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

public abstract class InputConfiguration implements Configuration {
    @NotBlank
    private final String id;

    @NotNull
    private Set<String> outputs = Sets.newHashSet();

    public InputConfiguration(String id, Config config) {
        this.id = id;

        if (config.hasPath("outputs")) {
            this.outputs = Sets.newHashSet(Splitter.on(",").omitEmptyStrings().trimResults().split(config.getString("outputs")));
        }
    }

    @Override
    public String getId() {
        return id;
    }

    public Set<String> getOutputs() {
        return outputs;
    }

    @Override
    public Map<String, String> toStringValues() {
        return Collections.unmodifiableMap(new HashMap<String, String>() {
            {
                put("id", getId());
                put("outputs", Joiner.on(",").join(getOutputs()));
            }
        });
    }
}
