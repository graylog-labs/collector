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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import org.graylog.collector.config.Configuration;
import com.typesafe.config.Config;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class InputConfiguration implements Configuration {
    public interface Factory<C extends InputConfiguration> {
        C create(String id, Config config);
    }

    @NotBlank
    private final String id;

    @NotNull
    private Set<String> outputs = Sets.newHashSet();

    public InputConfiguration(String id, Config config) {
        this.id = id;

        if (config.hasPath("outputs")) {
            this.outputs = Sets.newHashSet(Splitter.on(",").omitEmptyStrings().trimResults().split(config.getString("outputs")));
        }
    }

    public abstract InputService createInput();

    @Override
    public String getId() {
        return id;
    }

    public Set<String> getOutputs() {
        return outputs;
    }

    @Override
    public Map<String, String> toStringValues() {
        return Collections.unmodifiableMap(new HashMap<String, String>() {
            {
                put("id", getId());
                put("outputs", Joiner.on(",").join(getOutputs()));
            }
        });
    }
}
