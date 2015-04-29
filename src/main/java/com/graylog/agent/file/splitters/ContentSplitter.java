package com.graylog.agent.file.splitters;

import io.netty.buffer.ByteBuf;

import java.nio.charset.Charset;

public abstract class ContentSplitter {

    public abstract Iterable<String> split(ByteBuf buffer, Charset charset, boolean includeRemainingData);

    public Iterable<String> split(ByteBuf buffer, Charset charset) {
        return split(buffer, charset, false);
    }

    public Iterable<String> splitRemaining(ByteBuf buffer, Charset charset) {
        return split(buffer, charset, true);
    }
}
