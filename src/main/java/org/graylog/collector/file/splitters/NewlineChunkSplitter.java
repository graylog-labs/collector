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
package org.graylog.collector.file.splitters;

import com.google.common.collect.AbstractIterator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;
import org.graylog.collector.file.FileChunkBuffer;
import org.graylog.collector.file.FileChunkMessage;

import java.nio.charset.Charset;
import java.util.Iterator;

public class NewlineChunkSplitter extends ContentSplitter {
    @Override
    public Iterable<FileChunkMessage> split(final FileChunkBuffer buffer, final Charset charset, final boolean includeRemainingData) {
        return new Iterable<FileChunkMessage>() {
            @Override
            public Iterator<FileChunkMessage> iterator() {
                return new AbstractIterator<FileChunkMessage>() {

                    @Override
                    protected FileChunkMessage computeNext() {
                        try {
                            if (!buffer.isReadable()) {
                                return endOfData();
                            }
                            final int i = buffer.forEachByte(ByteBufProcessor.FIND_CRLF);
                            if (i == -1) {
                                if (includeRemainingData) {
                                    final long startOffset = buffer.getFileOffset();
                                    final ByteBuf remaining = buffer.readBytes(buffer.readableBytes());
                                    return new FileChunkMessage(remaining.toString(charset), buffer.getPath(), startOffset, buffer.getFileOffset() - startOffset);
                                } else {
                                    return endOfData();
                                }
                            }
                            final long startOffset = buffer.getFileOffset();
                            final ByteBuf fullLine = buffer.readBytes(i);
                            // Strip the \r/\n bytes from the buffer.
                            final byte readByte = buffer.readByte(); // the \r or \n byte
                            if (readByte == '\r') {
                                buffer.readByte(); // the \n byte if previous was \r
                            }
                            return new FileChunkMessage(fullLine.toString(charset), buffer.getPath(), startOffset, buffer.getFileOffset() - startOffset);
                        } finally {
                            buffer.discardReadBytes();
                        }
                    }
                };
            }
        };
    }
}
