/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.collector;

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

    @Override
    public String toString() {
        return "v" + version() + "(commit "+ commitIdShort() + ")";
    }
}
