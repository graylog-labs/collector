package com.graylog.agent.file.splitters;

import com.google.common.base.Charsets;
import com.graylog.agent.inputs.file.FileInputConfiguration;
import com.typesafe.config.Config;
import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;

public abstract class ContentSplitter {

    public abstract Iterable<String> split(ByteBuf buffer, Charset charset, boolean includeRemainingData);

    /**
     * Convenience method for {@link ContentSplitter#split(org.jboss.netty.buffer.ChannelBuffer, java.nio.charset.Charset, boolean)} with the character set UTF-8.
     *
     * @param buffer
     * @return
     */
    public Iterable<String> split(ByteBuf buffer) {
        return split(buffer, Charsets.UTF_8, false);
    }

    public Iterable<String> splitRemaining(ByteBuf buffer) {
        return split(buffer, Charsets.UTF_8, true);
    }
}
