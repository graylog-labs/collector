/*
 * Copyright 2014 TORCH GmbH
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
package com.graylog.agent.file.splitters;

import com.google.common.collect.AbstractIterator;
import com.graylog.agent.file.compat.Configuration;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferIndexFinder;

import java.nio.charset.Charset;
import java.util.Iterator;

public class NewlineChunkSplitter extends ContentSplitter {
    @Override
    public void configure(Configuration configuration) {
    }

    @Override
    public Iterable<String> split(final ChannelBuffer buffer, final Charset charset, final boolean includeRemainingData) {
        return new Iterable<String>() {
            @Override
            public Iterator<String> iterator() {
                return new AbstractIterator<String>() {

                    @Override
                    protected String computeNext() {
                        try {
                            if (!buffer.readable()) {
                                return endOfData();
                            }
                            final int i = buffer.bytesBefore(ChannelBufferIndexFinder.LF);
                            if (i == -1) {
                                if (includeRemainingData) {
                                    final ChannelBuffer remaining = buffer.readBytes(buffer.readableBytes());
                                    return remaining.toString(charset);
                                } else {
                                    return endOfData();
                                }
                            }
                            final ChannelBuffer fullLine = buffer.readBytes(i);
                            buffer.readByte(); // the newline byte
                            return fullLine.toString(charset);
                        } finally {
                            buffer.discardReadBytes();
                        }
                    }
                };
            }
        };
    }

}
