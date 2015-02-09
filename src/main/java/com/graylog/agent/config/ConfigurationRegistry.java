package com.graylog.agent.config;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Service;
import com.graylog.agent.inputs.Input;
import com.graylog.agent.inputs.InputConfiguration;
import com.graylog.agent.inputs.InputService;
import com.graylog.agent.outputs.Output;
import com.graylog.agent.outputs.OutputConfiguration;
import com.graylog.agent.outputs.OutputService;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigurationRegistry {
    private final Set<Service> services = Sets.newHashSet();
    private final Set<Input> inputs = Sets.newHashSet();
    private final Set<Output> outputs = Sets.newHashSet();

    private final Map<String, InputConfiguration.Factory<? extends InputConfiguration>> inputConfigFactories;
    private final Map<String, OutputConfiguration.Factory<? extends OutputConfiguration>> outputConfigFactories;

    private final List<ConfigurationError> errors = Lists.newArrayList();
    private final ConfigurationValidator validator;

    @Inject
    public ConfigurationRegistry(Config config,
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
        final Map<String, InputConfiguration.Factory<? extends InputConfiguration>> factories = ConfigurationRegistry.this.inputConfigFactories;

        dispatchConfig(inputConfigs, new ConfigCallback() {
            @Override
            public void call(String type, String id, Config config) {
                if (factories.containsKey(type)) {
                    final InputConfiguration cfg = factories.get(type).create(id, config);

                    if (validator.isValid(cfg)) {
                        final InputService input = cfg.createInput();
                        services.add(input);
                        inputs.add(input);
                    }
                } else {
                    errors.add(new ConfigurationError("Unknown input type \"" + type + "\" for " + id));
                }
            }
        });
    }

    private void buildOutputs(Config outputConfigs) {
        final Map<String, OutputConfiguration.Factory<? extends OutputConfiguration>> factories = ConfigurationRegistry.this.outputConfigFactories;

        dispatchConfig(outputConfigs, new ConfigCallback() {
            @Override
            public void call(String type, String id, Config config) {
                if (factories.containsKey(type)) {
                    final OutputConfiguration cfg = factories.get(type).create(id, config);

                    if (validator.isValid(cfg)) {
                        final OutputService output = cfg.createOutput();
                        services.add(output);
                        outputs.add(output);
                    }
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

    public Set<Input> getInputs() {
        return inputs;
    }

    public Set<Output> getOutputs() {
        return outputs;
    }

    private interface ConfigCallback {
        void call(String type, String id, Config config);
    }
}
