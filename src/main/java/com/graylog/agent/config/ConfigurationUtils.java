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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;

import javax.annotation.Nullable;
import java.util.Map;

public class ConfigurationUtils {
    public static String toString(Configuration configurationObject) {
        return toString(configurationObject, configurationObject);
    }

    public static String toString(Configuration configurationObject, Object nameClass) {
        final Map<String, String> values = configurationObject.toStringValues();
        final Iterable<String> strings = Iterables.transform(values.entrySet(), new Function<Map.Entry<String, String>, String>() {
            @Nullable
            @Override
            public String apply(@Nullable Map.Entry<String, String> input) {
                if (input == null) {
                    return "";
                }
                return String.format("%s='%s'", input.getKey(), input.getValue());
            }
        });

        final StringBuffer sb = new StringBuffer(nameClass.getClass().getSimpleName());
        sb.append('{');
        sb.append(Joiner.on(", ").join(strings));
        sb.append('}');

        return sb.toString();
    }
}
