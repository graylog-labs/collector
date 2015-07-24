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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Implements a circular {@link ByteBuf} with a fixed size. Old data gets overwritten.
 *
 * This class is thread-safe.
 */
public class CircularByteBuf {
    private final int size;
    private final ByteBuf buf;
    private int start;

    /**
     * Creates a new buffer with the given size.
     *
     * @param size fixed size of the buffer
     */
    public CircularByteBuf(final int size) {
        this.size = size;
        this.buf = Unpooled.buffer(size, size);
        this.start = 0;
    }

    /**
     * Write the given bytes to the buffer. If the data is bigger than the buffer, only write the last buffer-size
     * bytes into the buffer.
     *
     * @param bytes the data to write
     */
    public void write(final byte[] bytes) {
        synchronized (this) {
            if (bytes.length >= size) {
                // If the byte size is bigger or equals the buffer, just write the last <size> bytes into the buffer.
                // Clear writer and reader index as well as start position. All data in the buffer has been overwritten.
                buf.clear();
                buf.writeBytes(bytes, bytes.length - size, size);
                start = 0;
            } else if (bytes.length <= buf.writableBytes()) {
                // If the bytes fits into the remaining buffer, just write all of it.
                buf.writeBytes(bytes);

                if (start > 0) {
                    // Only increase the start pointer if we modified it before. This indicates that the buffer has been
                    // filled at least once.
                    start = buf.writerIndex();
                }
            } else {
                int writableBytes = buf.writableBytes();

                // Fill up the remaining buffer with the first part of the bytes.
                buf.writeBytes(bytes, 0, writableBytes);
                // Reset writer index to the beginning of the buffer.
                buf.writerIndex(0);
                // Write the rest of the bytes into the buffer.
                buf.writeBytes(bytes, writableBytes, bytes.length - writableBytes);

                // Set the start position because the oldest data in the buffer has been overwritten.
                start = buf.writerIndex();
            }
        }
    }

    /**
     * Reads the complete buffer and returns a byte array of size buffer-size. It always returns the full size byte
     * array, even if the buffer never has been filled. (padded with null-bytes at the end)
     *
     * @return the buffer as byte-array
     */
    public byte[] read() {
        final byte[] bytes = new byte[size];

        synchronized (this) {
            final int savedWriterIndex = buf.writerIndex();

            // Reset reader index to the beginning of the buffer so we can read all data.
            buf.readerIndex(0);
            // Reset writer index to the end of the buffer so we can read all data.
            // (writer index always has to be >= reader index)
            buf.writerIndex(size);

            // Put the newest data from the buffer to the end of the returned bytes.
            buf.readBytes(bytes, size - start, start);
            // Put the oldest data from the buffer at the beginning of the returned bytes.
            buf.readBytes(bytes, 0, size - start);

            // Reset reader index to 0 and the restore the saved writer index.
            buf.readerIndex(0);
            buf.writerIndex(savedWriterIndex);
        }

        return bytes;
    }
}
