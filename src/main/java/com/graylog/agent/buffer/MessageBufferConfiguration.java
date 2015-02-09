package com.graylog.agent.buffer;

import com.typesafe.config.Config;

import javax.inject.Inject;

public class MessageBufferConfiguration {
    private static int SIZE = 128;

    private final int size;

    @Inject
    public MessageBufferConfiguration(Config config) {
        if (config.hasPath("message-buffer-size")) {
            this.size = config.getInt("message-buffer-size");
        } else {
            this.size = SIZE;
        }
    }

    public int getSize() {
        return size;
    }
}
