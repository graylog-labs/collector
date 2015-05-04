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
package com.graylog.agent.outputs;

import com.graylog.agent.Message;
import com.graylog.agent.config.Configuration;

import java.util.Set;

public interface Output {
    String getId();

    Set<String> getInputs();

    void write(Message message);

    public interface Factory<T extends Output, C extends Configuration> {
        T create(C configuration);
    }
}
