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
package org.graylog.collector.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CircularByteBufTest {
    @Test
    public void test() throws Exception {
        final CircularByteBuf buf = new CircularByteBuf(10);

        assertEquals(10, buf.read().length);
        assertEquals("\0\0\0\0\0\0\0\0\0\0", new String(buf.read()));

        buf.write("01234".getBytes());

        assertEquals("01234\0\0\0\0\0", new String(buf.read()));

        buf.write("56789".getBytes());

        assertEquals("0123456789", new String(buf.read()));

        buf.write("over".getBytes());

        assertEquals("456789over", new String(buf.read()));

        buf.write("over".getBytes());

        assertEquals("89overover", new String(buf.read()));

        buf.write("over".getBytes());

        assertEquals("eroverover", new String(buf.read()));

        buf.write("0123456789".getBytes());

        assertEquals("0123456789", new String(buf.read()));

        buf.write("a".getBytes());

        assertEquals("123456789a", new String(buf.read()));

        buf.write("bc".getBytes());

        assertEquals("3456789abc", new String(buf.read()));

        buf.write("".getBytes());

        assertEquals("3456789abc", new String(buf.read()));

        buf.write("0123456789abcdef".getBytes());

        assertEquals("6789abcdef", new String(buf.read()));

        buf.write("a\0b".getBytes());

        assertEquals("9abcdefa\0b", new String(buf.read()));

        buf.write("hällö".getBytes());

        assertEquals("a\0bhällö", new String(buf.read()));
    }
}