package com.graylog.agent.config;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Service;
import com.graylog.agent.inputs.InputConfiguration;
import com.graylog.agent.outputs.OutputConfiguration;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigurationProcessor {
    private final Set<Service> services = Sets.newHashSet();

    private final Map<String, InputConfiguration.Factory<? extends InputConfiguration>> inputConfigFactories;
    private final Map<String, OutputConfiguration.Factory<? extends OutputConfiguration>> outputConfigFactories;

    private final List<ConfigurationError> errors = Lists.newArrayList();
    private final ConfigurationValidator validator;

    @Inject
    public ConfigurationProcessor(Config config,
                                  Map<String, InputConfiguration.Factory<? extends InputConfiguration>> inputConfigs,
                                  Map<String, OutputConfiguration.Factory<? extends OutputConfiguration>> outputConfigs) {
        this.inputConfigFactories = inputConfigs;
        this.outputConfigFactories = outputConfigs;
        this.validator = new ConfigurationValidator();

        try {
            processConfig(config);
        } catch (ConfigException e) {
            errors.add(new ConfigurationError(e.getMessage()));
        }
    }

    private void processConfig(Config config) {
        final Config inputs = config.getConfig("inputs");
        final Config outputs = config.getConfig("outputs");

        buildInputs(inputs);
        buildOutputs(outputs);

        errors.addAll(validator.getErrors());
    }

    private void buildInputs(final Config inputConfigs) {
        final Map<String, InputConfiguration.Factory<? extends InputConfiguration>> factories = ConfigurationProcessor.this.inputConfigFactories;

        dispatchConfig(inputConfigs, new ConfigCallback() {
            @Override
            public void call(String type, String id, Config config) {
                if (factories.containsKey(type)) {
                    final InputConfiguration cfg = factories.get(type).create(id, config);
                    services.add(cfg.createInput());
                } else {
                    errors.add(new ConfigurationError("Unknown input type \"" + type + "\" for " + id));
                }
            }
        });
    }

    private void buildOutputs(Config outputConfigs) {
        final Map<String, OutputConfiguration.Factory<? extends OutputConfiguration>> factories = ConfigurationProcessor.this.outputConfigFactories;

        dispatchConfig(outputConfigs, new ConfigCallback() {
            @Override
            public void call(String type, String id, Config config) {
                if (factories.containsKey(type)) {
                    final OutputConfiguration cfg = factories.get(type).create(id, config);
                    services.add(cfg.createOutput());
                } else {
                    errors.add(new ConfigurationError("Unknown output type \"" + type + "\" for " + id));
                }
            }
        });
    }

    private void dispatchConfig(Config config, ConfigCallback callback) {
        for (Map.Entry<String, ConfigValue> entry : config.root().entrySet()) {
            final String id = entry.getKey();

            try {
                final Config entryConfig = ((ConfigObject) entry.getValue()).toConfig();
                final String type = entryConfig.getString("type");

                if (Strings.isNullOrEmpty(type)) {
                    errors.add(new ConfigurationError("Missing type field for " + id + " (" + entryConfig + ")"));
                    continue;
                }

                callback.call(type, id, entryConfig);
            } catch (ConfigException e) {
                errors.add(new ConfigurationError("[" + id + "] " + e.getMessage()));
            }
        }
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public List<ConfigurationError> getErrors() {
        return errors;
    }

    public Set<Service> getServices() {
        return services;
    }

    private interface ConfigCallback {
        void call(String type, String id, Config config);
    }
}
