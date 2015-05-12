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

import com.google.common.base.Charsets;
import com.google.common.collect.AbstractIterator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;

import java.nio.charset.Charset;
import java.util.Iterator;

public class NewlineChunkSplitter extends ContentSplitter {
    @Override
    public Iterable<String> split(final ByteBuf buffer, final Charset charset, final boolean includeRemainingData) {
        return new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                return new AbstractIterator<String>() {

                    @Override
                    protected String computeNext() {
                        try {
                            if (!buffer.isReadable()) {
                                return endOfData();
                            }
                            final int i = buffer.forEachByte(ByteBufProcessor.FIND_CRLF);
                            if (i == -1) {
                                if (includeRemainingData) {
                                    final ByteBuf remaining = buffer.readBytes(buffer.readableBytes());
                                    return remaining.toString(charset);
                                } else {
                                    return endOfData();
                                }
                            }
                            final ByteBuf fullLine = buffer.readBytes(i);
                            // Strip the \r/\n bytes from the buffer.
                            final byte readByte = buffer.readByte();// the \r or \n byte
                            if (readByte == '\r') {
                                buffer.readByte(); // the \n byte if previous was \r
                            }
                            return new String(fullLine.toString(charset).getBytes(Charsets.UTF_8));
                        } finally {
                            buffer.discardReadBytes();
                        }
                    }
                };
            }
        };
    }
}
