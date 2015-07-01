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

import com.google.common.collect.Maps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.nio.file.Path;
import java.util.concurrent.ConcurrentMap;

/**
 * Stores {@link ByteBuf} chunks for a path. It appends new buffers to existing ones.
 *
 * This class is thread-safe.
 */
public class ChunkBufferStore {
    private final ConcurrentMap<Path, ByteBuf> buffersPerFile = Maps.newConcurrentMap();

    public ChunkBufferStore() {
    }

    /**
     * Returns the {@link ByteBuf} for the given path, null if there is no buffer for the path.
     *
     * @param path the path
     * @return the buffer for the given path or null
     */
    public ByteBuf get(final Path path) {
        return buffersPerFile.get(path);
    }

    /**
     * Stores the {@link ByteBuf} for the given path. If there is a buffer for the path already, the new buffer
     * will be appended to the existing one.
     *
     * @param path the path
     * @param chunk the buffer
     */
    public void put(final Path path, final ByteBuf chunk) {
        synchronized (this) {
            final ByteBuf buf = get(path);

            if (buf == null) {
                buffersPerFile.put(path, chunk);
            } else {
                buffersPerFile.put(path, Unpooled.wrappedBuffer(buf, chunk));
            }
        }
    }
}
