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
package com.graylog.agent.config;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.io.File;

public class ConfigurationParser {
    public static class Error extends RuntimeException {
        public Error(String message) {
            super(message);
        }
    }

    public static Config parse(File configFile) {
        Config config = null;

        if (configFile.exists() && configFile.canRead()) {
            config = ConfigFactory.parseFile(configFile);

            if (config.isEmpty()) {
                throw new Error("Empty configuration!");
            }
        } else {
            throw new Error("Configuration file " + configFile + " does not exist or is not readable!");
        }

        return config;
    }
}
