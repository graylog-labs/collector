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
package com.graylog.agent.inputs;

import com.graylog.agent.config.Configuration;
import com.graylog.agent.file.ChunkReader;

import java.util.Set;

public interface Input {
    String getId();

    Set<String> getOutputs();

    // TODO Check if needed and for what it was used.
    void setReaderFinished(ChunkReader chunkReader);

    public interface Factory<T extends Input, C extends Configuration> {
        T create(C configuration);
    }
}
