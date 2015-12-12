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
package org.graylog.collector;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import java.util.Map;

public class MessageFields {
    private final Map<String, Number> numberFields = Maps.newConcurrentMap();
    private final Map<String, Boolean> booleanFields = Maps.newConcurrentMap();
    private final Map<String, String> stringFields = Maps.newConcurrentMap();

    public MessageFields(Map<String, Number> numberFields,
                         Map<String, Boolean> booleanFields,
                         Map<String, String> stringFields) {
        this.numberFields.putAll(numberFields);
        this.booleanFields.putAll(booleanFields);
        this.stringFields.putAll(stringFields);
    }

    public MessageFields() {
    }

    public void put(String key, Number value) {
        numberFields.put(key, value);
    }

    public void put(String key, boolean value) {
        booleanFields.put(key, value);
    }

    public void put(String key, String value) {
        stringFields.put(key, value);
    }

    public Map<String, Object> asMap() {
        return ImmutableMap.<String, Object>builder()
                .putAll(numberFields)
                .putAll(booleanFields)
                .putAll(stringFields)
                .build();
    }

    @Override
    public String toString() {
        final MoreObjects.ToStringHelper stringHelper = MoreObjects.toStringHelper(this);

        for (Map.Entry<String, Object> entry : asMap().entrySet()) {
            stringHelper.add(entry.getKey(), entry.getValue());
        }

        return stringHelper.toString();
    }

    public MessageFields copy() {
        return new MessageFields(numberFields, booleanFields, stringFields);
    }
}
