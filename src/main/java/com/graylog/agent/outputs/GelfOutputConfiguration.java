package com.graylog.agent.outputs;

import com.graylog.agent.config.Configuration;
import com.graylog.agent.config.constraints.IsOneOf;
import com.typesafe.config.Config;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.Range;

import javax.validation.constraints.NotNull;

public class GelfOutputConfiguration implements Configuration {
    @NotBlank
    private final String id;

    @NotBlank
    @IsOneOf({"tcp", "udp"})
    private String protocol;

    @NotBlank
    private String host;

    @NotNull
    @Range(min = 1024, max = 65535)
    private int port;

    public GelfOutputConfiguration(String id, Config config) {
        this.id = id;

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
    public String getId() {
        return id;
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
    public String toString() {
        return "GelfOutputConfiguration{" +
                "id='" + id + '\'' +
                ", protocol='" + protocol + '\'' +
                ", host='" + host + '\'' +
                ", port='" + port + '\'' +
                '}';
    }

}
