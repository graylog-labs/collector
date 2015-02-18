package com.graylog.agent;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static com.google.common.base.Strings.isNullOrEmpty;

public class AgentVersion {
    public static final AgentVersion CURRENT;

    static {
        String version = "NONE";
        String commitId = "NONE";
        String commitIdShort = "NONE";
        String timestamp = "NONE";

        final InputStream stream = AgentVersion.class.getResourceAsStream("/agent-version.properties");
        final Properties properties = new Properties();

        try {
            properties.load(stream);

            version = properties.getProperty("version");
            commitId = properties.getProperty("commit-id");
            timestamp = properties.getProperty("timestamp");

            if (!isNullOrEmpty(commitId) && !"NONE".equals(commitId)) {
                commitIdShort = commitId.substring(0, 7);
            }
        } catch (IOException ignored) {
        }

        CURRENT = new AgentVersion(version, commitId, commitIdShort, timestamp);
    }

    private final String version;
    private final String commitId;
    private final String commitIdShort;
    private final String timestamp;

    public AgentVersion(String version, String commitId, String commitIdShort, String timestamp) {
        this.version = version;
        this.commitId = commitId;
        this.commitIdShort = commitIdShort;
        this.timestamp = timestamp;
    }

    public String version() {
        return version;
    }

    public String commitId() {
        return commitId;
    }

    public String commitIdShort() {
        return commitIdShort;
    }

    public String timestamp() {
        return timestamp;
    }
}
