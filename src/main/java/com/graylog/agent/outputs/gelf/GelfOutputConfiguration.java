package com.graylog.agent.outputs.gelf;

import com.google.inject.assistedinject.Assisted;
import com.graylog.agent.annotations.AgentConfigurationFactory;
import com.graylog.agent.annotations.AgentOutputConfiguration;
import com.graylog.agent.config.ConfigurationUtils;
import com.graylog.agent.config.constraints.IsOneOf;
import com.graylog.agent.outputs.OutputConfiguration;
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

    @NotNull
    @Range(min = 1, max = Integer.MAX_VALUE)
    private int clientQueueSize = 512;

    @NotNull
    @Range(min = 1, max = Integer.MAX_VALUE)
    private int clientConnectTimeout = 5000;

    @NotNull
    @Range(min = 1, max = Integer.MAX_VALUE)
    private int clientReconnectDelay = 1000;

    @NotNull
    private boolean clientTcpNoDelay = true;

    @NotNull
    @Range(min = -1, max = Integer.MAX_VALUE)
    private int clientSendBufferSize = -1;

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
        if (config.hasPath("client-queue-size")) {
            this.clientQueueSize = config.getInt("client-queue-size");
        }
        if (config.hasPath("client-connect-timeout")) {
            this.clientConnectTimeout = config.getInt("client-connect-timeout");
        }
        if (config.hasPath("client-reconnect-delay")) {
            this.clientReconnectDelay = config.getInt("client-reconnect-delay");
        }
        if (config.hasPath("client-tcp-no-delay")) {
            this.clientTcpNoDelay = config.getBoolean("client-tcp-no-delay");
        }
        if (config.hasPath("client-send-buffer-size")) {
            this.clientSendBufferSize = config.getInt("client-send-buffer-size");
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

    public int getClientQueueSize() {
        return clientQueueSize;
    }

    public int getClientConnectTimeout() {
        return clientConnectTimeout;
    }

    public int getClientReconnectDelay() {
        return clientReconnectDelay;
    }

    public boolean isClientTcpNoDelay() {
        return clientTcpNoDelay;
    }

    public int getClientSendBufferSize() {
        return clientSendBufferSize;
    }

    @Override
    public Map<String, String> toStringValues() {
        return Collections.unmodifiableMap(new HashMap<String, String>(super.toStringValues()) {
            {
                put("protocol", getProtocol());
                put("host", getHost());
                put("port", String.valueOf(getPort()));
                put("client-queue-size", String.valueOf(getClientQueueSize()));
                put("client-connect-timeout", String.valueOf(getClientConnectTimeout()));
                put("client-reconnect-delay", String.valueOf(getClientReconnectDelay()));
                put("client-tcp-no-delay", String.valueOf(isClientTcpNoDelay()));
                put("client-send-buffer-size", String.valueOf(getClientSendBufferSize()));
            }
        });
    }

    @Override
    public String toString() {
        return ConfigurationUtils.toString(this);
    }
}
