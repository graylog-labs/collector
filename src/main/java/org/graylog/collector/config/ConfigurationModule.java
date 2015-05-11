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
package org.graylog.collector.config;

import com.google.inject.Scopes;
import org.graylog.collector.guice.AgentModule;
import com.typesafe.config.Config;

import java.io.File;

public class ConfigurationModule extends AgentModule {
    private final Config config;

    public ConfigurationModule(File configFile) {
        this.config = ConfigurationParser.parse(configFile);
    }

    @Override
    protected void configure() {
        bind(Config.class).toInstance(config);
        bind(ConfigurationRegistry.class).in(Scopes.SINGLETON);
    }
}
