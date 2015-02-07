package com.graylog.agent.outputs;

import com.graylog.agent.utils.ConfigurationUtils;
import com.graylog.agent.config.constraints.IsOneOf;
import com.typesafe.config.Config;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GelfOutputConfiguration extends OutputConfiguration {
    @NotBlank
    @IsOneOf({"tcp", "udp"})
    private String protocol;

    @NotBlank
    private String host;

    @NotNull
    @Range(min = 1024, max = 65535)
    private int port;

    public GelfOutputConfiguration(String id, Config config) {
        super(id, config);

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
