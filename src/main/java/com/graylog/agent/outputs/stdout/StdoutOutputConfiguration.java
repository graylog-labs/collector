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
package com.graylog.agent.outputs.stdout;

import com.google.inject.assistedinject.Assisted;
import com.graylog.agent.config.ConfigurationUtils;
import com.graylog.agent.outputs.OutputConfiguration;
import com.typesafe.config.Config;

import javax.inject.Inject;

public class StdoutOutputConfiguration extends OutputConfiguration {
    private final StdoutOutput.Factory outputFactory;

    public interface Factory extends OutputConfiguration.Factory<StdoutOutputConfiguration> {
        @Override
        StdoutOutputConfiguration create(String id, Config config);
    }

    @Inject
    public StdoutOutputConfiguration(@Assisted String id,
                                     @Assisted Config output,
                                     StdoutOutput.Factory outputFactory) {
        super(id, output);
        this.outputFactory = outputFactory;
    }

    @Override
    public StdoutOutput createOutput() {
        return outputFactory.create(this);
    }

    @Override
    public String toString() {
        return ConfigurationUtils.toString(this);
    }
}
