package com.graylog.agent.file.splitters;

import com.google.common.collect.AbstractIterator;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufProcessor;

import java.nio.charset.Charset;
import java.util.Iterator;

public class NewlineChunkSplitter extends ContentSplitter {
    public enum LineEnding {
        LF, CRLF;
    }

    private final LineEnding lineEnding;
    private final ByteBufProcessor processor;

    public NewlineChunkSplitter() {
        this(LineEnding.LF);
    }

    public NewlineChunkSplitter(LineEnding lineEnding) {
        this.lineEnding = lineEnding;
        this.processor = getProcessor(lineEnding);
    }

    public ByteBufProcessor getProcessor(LineEnding lineEnding) {
        switch (lineEnding) {
            case LF:
                return ByteBufProcessor.FIND_LF;
            case CRLF:
                return ByteBufProcessor.FIND_CRLF;
            default:
                throw new IllegalArgumentException("Unknown line ending");
        }
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
                            buffer.readByte(); // the newline/cr byte
                            if (lineEnding == LineEnding.CRLF) {
                                buffer.readByte(); // the newline byte if CRLF line endings are used
                            }
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
