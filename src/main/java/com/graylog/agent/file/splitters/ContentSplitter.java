package com.graylog.agent.file.splitters;

import com.google.common.base.Charsets;
import com.graylog.agent.file.compat.Configuration;
import org.jboss.netty.buffer.ChannelBuffer;

import java.nio.charset.Charset;

public abstract class ContentSplitter {

    public abstract void configure(Configuration configuration);

    public abstract Iterable<String> split(ChannelBuffer buffer, Charset charset, boolean includeRemainingData);

    /**
     * Convenience method for {@link ContentSplitter#split(org.jboss.netty.buffer.ChannelBuffer, java.nio.charset.Charset, boolean)} with the character set UTF-8.
     *
     * @param buffer
     * @return
     */
    public Iterable<String> split(ChannelBuffer buffer) {
        return split(buffer, Charsets.UTF_8, false);
    }

    public Iterable<String> splitRemaining(ChannelBuffer buffer) {
        return split(buffer, Charsets.UTF_8, true);
    }
}
