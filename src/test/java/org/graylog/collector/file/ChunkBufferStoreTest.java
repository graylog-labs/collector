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

import io.netty.buffer.Unpooled;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static com.google.common.base.Charsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ChunkBufferStoreTest {
    @Test
    public void test() throws Exception {
        final ChunkBufferStore store = new ChunkBufferStore();
        final Path path1 = Paths.get("/tmp/foo-1.log");
        final Path path2 = Paths.get("/tmp/foo-2.log");

        assertNull("Empty store should not have any buffers...", store.get(path1));
        assertNull("Empty store should not have any buffers...", store.get(path2));

        store.put(new FileChunk(path1, Unpooled.copiedBuffer("log 1\n".getBytes()), 0));
        assertEquals("Buffer should have correct data", "log 1\n", store.get(path1).toString(UTF_8));

        store.put(new FileChunk(path2, Unpooled.copiedBuffer("hello\n".getBytes()), 0));
        assertEquals("Buffer should have correct data", "hello\n", store.get(path2).toString(UTF_8));

        store.put(new FileChunk(path1, Unpooled.copiedBuffer("log 2\n".getBytes()), 0));
        assertEquals("New data should be appended to buffer", "log 1\nlog 2\n", store.get(path1).toString(UTF_8));

        store.get(path1).readBytes(store.get(path1).readableBytes());
        store.put(new FileChunk(path1, Unpooled.copiedBuffer("new log\n".getBytes()), 0));
        assertEquals("Writing to truncated buffer should only have new data", "new log\n", store.get(path1).toString(UTF_8));

        store.get(path2).readBytes(3);
        store.put(new FileChunk(path2, Unpooled.copiedBuffer("new log\n".getBytes()), 0));
        assertEquals("Buffer should have rest of old and the new data", "lo\nnew log\n", store.get(path2).toString(UTF_8));
        assertEquals(3, store.get(path2).getFileOffset());
    }
}