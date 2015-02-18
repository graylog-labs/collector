package com.graylog.agent.file.splitters;

import com.google.common.collect.AbstractIterator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;

import java.nio.charset.Charset;
import java.util.Iterator;

public class NewlineChunkSplitter extends ContentSplitter {
    @Override
    public void configure(ContentSplitterConfiguration configuration) {
    }

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
                            final int i = buffer.forEachByte(ByteBufProcessor.FIND_LF);
                            if (i == -1) {
                                if (includeRemainingData) {
                                    final ByteBuf remaining = buffer.readBytes(buffer.readableBytes());
                                    return remaining.toString(charset);
                                } else {
                                    return endOfData();
                                }
                            }
                            final ByteBuf fullLine = buffer.readBytes(i);
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
