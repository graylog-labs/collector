/*
 * Copyright 2013 TORCH GmbH
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.graylog.agent.file;

import org.jboss.netty.buffer.ChannelBuffer;

import java.nio.file.Path;

public class FileChunk {

    private final Path path;
    private final ChannelBuffer chunkBuffer;
    private final long id;

    public FileChunk(Path path, ChannelBuffer chunkBuffer, long id) {
        this.path = path;
        this.chunkBuffer = chunkBuffer;
        this.id = id;
    }

    public static FileChunk finalChunk(Path path) {
        return new FileChunk(path, null, -1);
    }

    public Path getPath() {
        return path;
    }

    public ChannelBuffer getChunkBuffer() {
        return chunkBuffer;
    }

    public long getId() {
        return id;
    }

    public boolean isFinalChunk() {
        return chunkBuffer == null;
    }
}
