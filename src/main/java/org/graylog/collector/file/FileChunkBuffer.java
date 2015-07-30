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

import com.google.common.base.MoreObjects;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import io.netty.buffer.Unpooled;

import java.nio.charset.Charset;
import java.nio.file.Path;

/**
 * This buffer holds a chunk of a file. It maintains an offset that represents the offset of the buffer in the
 * actual file.
 *
 * This class it not thread-safe!
 */
public class FileChunkBuffer {
    private final Path path;
    private ByteBuf buffer;
    private long fileOffset;
    private long bufferEndoffset;

    /**
     * Creates a new buffer with the given path, byte-buffer and file offset.
     *
     * @param path       the file this buffer represents
     * @param buffer     content of the file chunk
     * @param fileOffset the offset of the buffer in the file
     */
    public FileChunkBuffer(final Path path, final ByteBuf buffer, final long fileOffset) {
        this.path = path;
        this.buffer = buffer;
        this.fileOffset = fileOffset;
        this.bufferEndoffset = fileOffset + buffer.readableBytes();
    }

    /**
     * Creates a new buffer for the given {@link FileChunk}.
     *
     * @param fileChunk the chunk to store
     */
    public FileChunkBuffer(final FileChunk fileChunk) {
        this(fileChunk.getPath(), fileChunk.getChunkBuffer(), fileChunk.getFileOffset());
    }

    /**
     * Returns the path for this buffer.
     *
     * @return the path to the file
     */
    public Path getPath() {
        return path;
    }

    /**
     * Returns the offset in the file where the buffer starts. This offset will change if the buffer is modified.
     *
     * @return the offset of the buffer in the file
     */
    public long getFileOffset() {
        return fileOffset;
    }

    /**
     * Appends the given {@link FileChunk} to the current one.
     *
     * @param fileChunk the chunk to add
     */
    public void append(final FileChunk fileChunk) {
        // Not adding a chunk for a different path to this buffer.
        if (!path.equals(fileChunk.getPath())) {
            return;
        }
        buffer = Unpooled.wrappedBuffer(buffer, fileChunk.getChunkBuffer());
        bufferEndoffset = fileOffset + buffer.readableBytes();
    }

    private void incrementProcessedOffset(final int i) {
        fileOffset += i;
    }

    private void resetProcessedOffset() {
        fileOffset = bufferEndoffset - buffer.readableBytes();
    }

    /**
     * Delegates to {@link ByteBuf#isReadable()} on the internal buffer.
     */
    public boolean isReadable() {
        return buffer.isReadable();
    }

    /**
     * Delegates to {@link ByteBuf#toString(Charset)} on the internal buffer.
     */
    public String toString(final Charset charset) {
        return buffer.toString(charset);
    }

    /**
     * Delegates to {@link ByteBuf#skipBytes(int)} on the internal buffer.
     */
    public FileChunkBuffer skipBytes(final int length) {
        buffer.skipBytes(length);
        incrementProcessedOffset(length);
        return this;
    }

    /**
     * Delegates to {@link ByteBuf#discardReadBytes()} on the internal buffer.
     */
    public FileChunkBuffer discardReadBytes() {
        buffer.discardReadBytes();
        resetProcessedOffset();
        return this;
    }

    /**
     * Delegates to {@link ByteBuf#readableBytes()} on the internal buffer.
     */
    public int readableBytes() {
        return buffer.readableBytes();
    }

    /**
     * Delegates to {@link ByteBuf#readBytes(int)} on the internal buffer.
     */
    public ByteBuf readBytes(final int length) {
        final ByteBuf buf = buffer.readBytes(length);
        incrementProcessedOffset(buf.readableBytes());
        return buf;
    }

    /**
     * Delegates to {@link ByteBuf#forEachByte(ByteBufProcessor)} on the internal buffer.
     */
    public int forEachByte(final ByteBufProcessor processor) {
        return buffer.forEachByte(processor);
    }

    /**
     * Delegates to {@link ByteBuf#readByte()} on the internal buffer.
     */
    public byte readByte() {
        incrementProcessedOffset(1);
        return buffer.readByte();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("path", path)
                .add("bufferLength", buffer.readableBytes())
                .add("fileOffset", fileOffset)
                .add("bufferEndoffset", bufferEndoffset)
                .toString();
    }
}
