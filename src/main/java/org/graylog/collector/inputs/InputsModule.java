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
package org.graylog.collector.inputs;

import org.graylog.collector.guice.AgentModule;
import org.graylog.collector.inputs.eventlog.WindowsEventlogInput;
import org.graylog.collector.inputs.eventlog.WindowsEventlogInputConfiguration;
import org.graylog.collector.inputs.file.FileInput;
import org.graylog.collector.inputs.file.FileInputConfiguration;
import org.graylog.collector.utils.Utils;

public class InputsModule extends AgentModule {
    @Override
    protected void configure() {
        registerInput("file",
                FileInput.class,
                FileInput.Factory.class,
                FileInputConfiguration.class,
                FileInputConfiguration.Factory.class);

        if (Utils.isWindows()) {
            registerInput("windows-eventlog",
                    WindowsEventlogInput.class,
                    WindowsEventlogInput.Factory.class,
                    WindowsEventlogInputConfiguration.class,
                    WindowsEventlogInputConfiguration.Factory.class);
        }
    }
}
