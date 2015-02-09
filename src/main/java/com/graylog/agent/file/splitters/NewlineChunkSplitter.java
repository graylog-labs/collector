package com.graylog.agent.file.splitters;

import com.google.common.collect.AbstractIterator;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferIndexFinder;

import java.nio.charset.Charset;
import java.util.Iterator;

public class NewlineChunkSplitter extends ContentSplitter {
    @Override
    public void configure(ContentSplitterConfiguration configuration) {
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
