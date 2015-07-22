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
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;
import org.graylog.collector.MessageFields;
import org.graylog.collector.config.Configuration;
import org.hibernate.validator.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class InputConfiguration implements Configuration {
    private static final Logger log = LoggerFactory.getLogger(InputConfiguration.class);

    public interface Factory<C extends InputConfiguration> {
        C create(String id, Config config);
    }

    @NotBlank
    private final String id;

    @NotNull
    private Set<String> outputs = Sets.newHashSet();

    private final MessageFields messageFields = new MessageFields();

    public InputConfiguration(String id, Config config) {
        this.id = id;

        if (config.hasPath("outputs")) {
            this.outputs = Sets.newHashSet(Splitter.on(",").omitEmptyStrings().trimResults().split(config.getString("outputs")));
        }

        if (config.hasPath("message-fields")) {
            final Config messageFieldsConfig = config.getConfig("message-fields");

            for (Map.Entry<String, ConfigValue> entry : messageFieldsConfig.entrySet()) {
                final String key = entry.getKey();
                final ConfigValue value = entry.getValue();

                switch (value.valueType()) {
                    case NUMBER:
                        this.messageFields.put(key, messageFieldsConfig.getLong(key));
                        break;
                    case BOOLEAN:
                        this.messageFields.put(key, messageFieldsConfig.getBoolean(key));
                        break;
                    case STRING:
                        this.messageFields.put(key, messageFieldsConfig.getString(key));
                        break;
                    default:
                        log.warn("{}[{}] Message field value of type \"{}\" is not supported for key \"{}\" (value: {})",
                                getClass().getSimpleName(), getId(), value.valueType(), key, value.toString());
                        break;
                }
            }
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

    public MessageFields getMessageFields() {
        return messageFields;
    }

    @Override
    public Map<String, String> toStringValues() {
        return Collections.unmodifiableMap(new HashMap<String, String>() {
            {
                put("id", getId());
                put("outputs", Joiner.on(",").join(getOutputs()));
                put("message-fields", getMessageFields().toString());
            }
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        InputConfiguration that = (InputConfiguration) o;

        return id.equals(that.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
