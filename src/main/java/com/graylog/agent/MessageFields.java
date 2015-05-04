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
package com.graylog.agent;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Map;

public class MessageFields {
    private final Map<String, Integer> intFields = Maps.newConcurrentMap();
    private final Map<String, Long> longFields = Maps.newConcurrentMap();
    private final Map<String, Boolean> booleanFields = Maps.newConcurrentMap();
    private final Map<String, String> stringFields = Maps.newConcurrentMap();

    public void put(String key, int value) {
        intFields.put(key, value);
    }

    public void put(String key, long value) {
        longFields.put(key, value);
    }

    public void put(String key, boolean value) {
        booleanFields.put(key, value);
    }

    public void put(String key, String value) {
        stringFields.put(key, value);
    }

    public Map<String, Object> asMap() {
        return ImmutableMap.<String, Object>builder()
                .putAll(intFields)
                .putAll(longFields)
                .putAll(booleanFields)
                .putAll(stringFields)
                .build();
    }
}
