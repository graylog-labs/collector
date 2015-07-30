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

import java.nio.file.Path;
import java.util.concurrent.ConcurrentMap;

/**
 * Stores {@link ByteBuf} chunks for a path. It appends new buffers to existing ones.
 *
 * This class is thread-safe.
 */
public class ChunkBufferStore {
    private final ConcurrentMap<Path, FileChunkBuffer> buffersPerFile = Maps.newConcurrentMap();

    public ChunkBufferStore() {
    }

    /**
     * Returns the {@link ByteBuf} for the given path, null if there is no buffer for the path.
     *
     * @param path the path
     * @return the buffer for the given path or null
     */
    public FileChunkBuffer get(final Path path) {
        return buffersPerFile.get(path);
    }

    /**
     * Stores the {@link ByteBuf} for the given {@link FileChunk}. If there is a buffer for the path already,
     * the new buffer will be appended to the existing one.
     *
     * @param chunk the file chunk object
     */
    public void put(final FileChunk chunk) {
        synchronized (this) {
            final FileChunkBuffer fileChunkBuffer = get(chunk.getPath());

            if (fileChunkBuffer == null) {
                buffersPerFile.put(chunk.getPath(), new FileChunkBuffer(chunk));
            } else {
                fileChunkBuffer.append(chunk);
            }
        }
    }
}
