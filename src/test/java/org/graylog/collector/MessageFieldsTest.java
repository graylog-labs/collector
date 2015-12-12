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

import org.junit.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

public class MessageFieldsTest {
    @Test
    public void testFields() {
        final MessageFields fields = new MessageFields();

        fields.put("bool", true);
        fields.put("int", 123);
        fields.put("long", 1500L);
        fields.put("double", 1.4D);
        fields.put("string", "string");

        final HashMap<String, Object> map = new HashMap<String, Object>() {
            {
                put("bool", true);
                put("int", 123);
                put("long", 1500L);
                put("double", 1.4D);
                put("string", "string");
            }
        };

        assertEquals(map, fields.asMap());
    }

    @Test
    public void testCopy() throws Exception {
        final MessageFields fields = new MessageFields();
        final MessageFields copy = fields.copy();

        fields.put("boolean", true);
        fields.put("number", 1);
        fields.put("string", "foo");
        copy.put("boolean", false);
        copy.put("number", 2);
        copy.put("string", "bar");

        assertThat(fields.asMap().get("boolean")).isEqualTo(true);
        assertThat(fields.asMap().get("number")).isEqualTo(1);
        assertThat(fields.asMap().get("string")).isEqualTo("foo");
        assertThat(copy.asMap().get("boolean")).isEqualTo(false);
        assertThat(copy.asMap().get("number")).isEqualTo(2);
        assertThat(copy.asMap().get("string")).isEqualTo("bar");
    }
}