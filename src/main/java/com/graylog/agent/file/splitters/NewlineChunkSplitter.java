package com.graylog.agent.file.splitters;

import com.google.common.collect.AbstractIterator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;

import java.nio.charset.Charset;
import java.util.Iterator;

public class NewlineChunkSplitter extends ContentSplitter {

    private final ByteBufProcessor processor;

    public NewlineChunkSplitter() {
        this(ByteBufProcessor.FIND_LF);
    }

    public NewlineChunkSplitter(ByteBufProcessor processor) {
        this.processor = processor;
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
                            final int i = buffer.forEachByte(processor);
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
