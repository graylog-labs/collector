package com.graylog.agent.config;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Service;
import com.graylog.agent.buffer.Buffer;
import com.graylog.agent.inputs.FileInput;
import com.graylog.agent.inputs.FileInputConfiguration;
import com.graylog.agent.outputs.GelfOutput;
import com.graylog.agent.outputs.GelfOutputConfiguration;
import com.graylog.agent.outputs.StdoutOutput;
import com.graylog.agent.outputs.StdoutOutputConfiguration;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigValue;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigurationProcessor {
    private final Set<Service> services = Sets.newHashSet();

    private final List<ConfigurationError> errors = Lists.newArrayList();
    private final ConfigurationValidator validator;
    private final Buffer buffer;

    public ConfigurationProcessor(Config config, Buffer buffer) {
        this.buffer = buffer;
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

    private void buildInputs(Config inputConfigs) {
        dispatchConfig(inputConfigs, new ConfigCallback() {
            @Override
            public void call(String type, String id, Config config) {
                switch (type) {
                    case "file":
                        final FileInputConfiguration fileCfg = new FileInputConfiguration(id, config);
                        if (validator.isValid(fileCfg)) {
                            services.add(new FileInput(fileCfg, buffer));
                        }
                        break;
                    default:
                        errors.add(new ConfigurationError("Unknown input type \"" + type + "\" for " + id));
                        break;
                }
            }
        });
    }

    private void buildOutputs(Config outputConfigs) {
        dispatchConfig(outputConfigs, new ConfigCallback() {
            @Override
            public void call(String type, String id, Config config) {
                switch (type) {
                    case "gelf":
                        final GelfOutputConfiguration gelfCfg = new GelfOutputConfiguration(id, config);
                        if (validator.isValid(gelfCfg)) {
                            services.add(new GelfOutput(gelfCfg, buffer));
                        }
                        break;
                    case "stdout":
                        final StdoutOutputConfiguration stdoutCfg = new StdoutOutputConfiguration(id, config);
                        if (validator.isValid(stdoutCfg)) {
                            services.add(new StdoutOutput(stdoutCfg, buffer));
                        }
                        break;
                    default:
                        errors.add(new ConfigurationError("Unknown output type \"" + type + "\" for " + id));
                        break;
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

    public static ConfigurationProcessor process(File configFile, Buffer buffer) {
        final Config config = ConfigFactory.parseFile(configFile);

        return new ConfigurationProcessor(config, buffer);
    }

    private interface ConfigCallback {
        void call(String type, String id, Config config);
    }
}
