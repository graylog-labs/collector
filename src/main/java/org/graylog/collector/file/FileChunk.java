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

import io.netty.buffer.ByteBuf;

import java.nio.file.Path;

public class FileChunk {

    private final Path path;
    private final ByteBuf chunkBuffer;
    private final long fileOffset;

    public FileChunk(Path path, ByteBuf chunkBuffer, long fileOffset) {
        this.path = path;
        this.chunkBuffer = chunkBuffer;
        this.fileOffset = fileOffset;
    }

    public static FileChunk finalChunk(Path path) {
        return new FileChunk(path, null, -1);
    }

    public Path getPath() {
        return path;
    }

    public long getFileOffset() {
        return fileOffset;
    }

    public ByteBuf getChunkBuffer() {
        return chunkBuffer;
    }

    public boolean isFinalChunk() {
        return chunkBuffer == null;
    }
}
