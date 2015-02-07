package com.graylog.agent.inputs;

import com.graylog.agent.utils.ConfigurationUtils;
import com.graylog.agent.config.constraints.IsAccessible;
import com.typesafe.config.Config;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FileInputConfiguration extends InputConfiguration {
    @NotNull
    @IsAccessible
    private File path;

    public FileInputConfiguration(String id, Config config) {
        super(id, config);

        if (config.hasPath("path")) {
            this.path = new File(config.getString("path"));
        }
    }

    public File getPath() {
        return path;
    }

    @Override
    public Map<String, String> toStringValues() {
        return Collections.unmodifiableMap(new HashMap<String, String>(super.toStringValues()) {
            {
                put("path", getPath().toString());
            }
        });
    }

    @Override
    public String toString() {
        return ConfigurationUtils.toString(this);
    }
}
