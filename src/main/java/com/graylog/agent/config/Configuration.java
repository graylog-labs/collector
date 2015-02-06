package com.graylog.agent.config;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.graylog.agent.inputs.FileInputConfiguration;
import com.graylog.agent.inputs.InputConfiguration;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Configuration {
    private final Set<FileInputConfiguration> fileInputConfigurations = Sets.newHashSet();
    private final List<InputConfiguration.ConfigurationError> errors = Lists.newArrayList();

    public Configuration(Config config) {
        final Config inputs = config.getConfig("inputs");

        for (Map.Entry<String, ConfigValue> entry : inputs.root().entrySet()) {
            final String id = entry.getKey();

            try {
                final Config input = ((ConfigObject) entry.getValue()).toConfig();
                final String type = input.getString("type");

                if (Strings.isNullOrEmpty(type)) {
                    errors.add(new InputConfiguration.ConfigurationError("Missing type field for " + id + " (" + input + ")"));
                    continue;
                }

                switch (type) {
                    case "file":
                        buildFileInputConfig(id, input);
                        break;
                    default:
                        errors.add(new InputConfiguration.ConfigurationError("Unknown input type \"" + type + "\" for " + id));
                        break;
                }
            } catch (ConfigException e) {
                errors.add(new InputConfiguration.ConfigurationError("[" + id + "] " + e.getMessage()));
            }
        }
    }

    private void buildFileInputConfig(String id, Config input) {
        final FileInputConfiguration config = new FileInputConfiguration(id, new File(input.getString("path")));
        final List<InputConfiguration.ConfigurationError> inputErrors = config.validate();

        if (inputErrors.isEmpty()) {
            fileInputConfigurations.add(config);
        } else {
            errors.addAll(inputErrors);
        }
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public List<InputConfiguration.ConfigurationError> getErrors() {
        return errors;
    }

    public static Configuration parse(File configFile) {
        final Config config = ConfigFactory.parseFile(configFile);

        return new Configuration(config);
    }

    public Set<FileInputConfiguration> getFileInputConfigurations() {
        return Collections.unmodifiableSet(fileInputConfigurations);
    }
}
