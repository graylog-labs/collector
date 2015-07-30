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
package org.graylog.collector.file;

import com.google.common.base.Charsets;
import io.netty.buffer.ByteBufProcessor;
import io.netty.buffer.Unpooled;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FileChunkBufferTest {
    private FileChunkBuffer buffer;
    private final Path path = Paths.get("/tmp/foo");

    @Before
    public void setUp() throws Exception {
        this.buffer = new FileChunkBuffer(path,
                Unpooled.copiedBuffer("this is the buffer content".getBytes()),
                0);
    }

    @Test
    public void testGetPath() throws Exception {
        assertEquals(path, buffer.getPath());
    }

    @Test
    public void testGetFileOffset() throws Exception {
        assertEquals(0, buffer.getFileOffset());

        buffer.readBytes(6);
        buffer.readBytes(4);

        assertEquals(10, buffer.getFileOffset());
    }

    @Test
    public void testAppend() throws Exception {
        buffer.append(new FileChunk(path, Unpooled.copiedBuffer(" more stuff".getBytes()), 26));

        assertEquals(0, buffer.getFileOffset());
        assertEquals("this is the buffer content more stuff", buffer.toString(Charsets.UTF_8));

        buffer.append(new FileChunk(Paths.get("eeek"), Unpooled.copiedBuffer(" not added".getBytes()), 0));

        assertEquals(0, buffer.getFileOffset());
        assertEquals("this is the buffer content more stuff", buffer.toString(Charsets.UTF_8));
    }

    @Test
    public void testIsReadable() throws Exception {
        assertTrue("Buffer should be readable", buffer.isReadable());
    }

    @Test
    public void testToString() throws Exception {
        assertEquals("this is the buffer content", buffer.toString(Charsets.UTF_8));
    }

    @Test
    public void testSkipBytes() throws Exception {
        buffer.skipBytes(10);

        assertEquals(10, buffer.getFileOffset());
        assertEquals("e buffer content", buffer.toString(Charsets.UTF_8));
    }

    @Test
    public void testDiscardReadBytes() throws Exception {
        buffer.readBytes(5);
        buffer.discardReadBytes();

        assertEquals(5, buffer.getFileOffset());
    }

    @Test
    public void testReadableBytes() throws Exception {
        assertEquals(26, buffer.readableBytes());

        buffer.append(new FileChunk(path, Unpooled.copiedBuffer("123".getBytes()), 26));

        assertEquals(29, buffer.readableBytes());
    }

    @Test
    public void testReadBytes() throws Exception {
        buffer.readBytes(5);

        assertEquals(5, buffer.getFileOffset());
    }

    @Test
    public void testForEachByte() throws Exception {
        assertEquals(4, buffer.forEachByte(ByteBufProcessor.FIND_LINEAR_WHITESPACE));
    }

    @Test
    public void testReadByte() throws Exception {
        assertEquals(116, buffer.readByte());
        assertEquals(104, buffer.readByte());
        assertEquals(2, buffer.getFileOffset());
    }
}