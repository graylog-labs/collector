package com.graylog.agent.outputs.gelf;

import com.google.inject.assistedinject.Assisted;
import com.graylog.agent.annotations.AgentConfigurationFactory;
import com.graylog.agent.annotations.AgentOutputConfiguration;
import com.graylog.agent.config.constraints.IsOneOf;
import com.graylog.agent.outputs.OutputConfiguration;
import com.graylog.agent.config.ConfigurationUtils;
import com.typesafe.config.Config;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.Range;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@AgentOutputConfiguration(type = "gelf")
public class GelfOutputConfiguration extends OutputConfiguration {
    private final GelfOutput.Factory outputFactory;

    @AgentConfigurationFactory
    public interface Factory extends OutputConfiguration.Factory<GelfOutputConfiguration> {
        @Override
        GelfOutputConfiguration create(String id, Config config);
    }

    @NotBlank
    @IsOneOf({"tcp", "udp"})
    private String protocol;

    @NotBlank
    private String host;

    @NotNull
    @Range(min = 1024, max = 65535)
    private int port;

    @Inject
    public GelfOutputConfiguration(@Assisted String id,
                                   @Assisted Config config,
                                   GelfOutput.Factory outputFactory) {
        super(id, config);
        this.outputFactory = outputFactory;

        if (config.hasPath("protocol")) {
            this.protocol = config.getString("protocol");
        }
        if (config.hasPath("host")) {
            this.host = config.getString("host");
        }
        if (config.hasPath("port")) {
            this.port = config.getInt("port");
        }
    }

    @Override
    public GelfOutput createOutput() {
        return outputFactory.create(this);
    }

    public String getProtocol() {
        return protocol;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    @Override
    public Map<String, String> toStringValues() {
        return Collections.unmodifiableMap(new HashMap<String, String>(super.toStringValues()) {
            {
                put("protocol", getProtocol());
                put("host", getHost());
                put("port", String.valueOf(getPort()));
            }
        });
    }

    @Override
    public String toString() {
        return ConfigurationUtils.toString(this);
    }
}
