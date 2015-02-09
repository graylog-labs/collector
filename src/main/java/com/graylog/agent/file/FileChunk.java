package com.graylog.agent.file;

import io.netty.buffer.ByteBuf;

import java.nio.file.Path;

public class FileChunk {

    private final Path path;
    private final ByteBuf chunkBuffer;
    private final long id;

    public FileChunk(Path path, ByteBuf chunkBuffer, long id) {
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

    public ByteBuf getChunkBuffer() {
        return chunkBuffer;
    }

    public long getId() {
        return id;
    }

    public boolean isFinalChunk() {
        return chunkBuffer == null;
    }
}
