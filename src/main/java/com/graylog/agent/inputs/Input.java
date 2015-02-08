package com.graylog.agent.inputs;

import com.graylog.agent.config.Configuration;
import com.graylog.agent.file.ChunkReader;

import java.util.Set;

public interface Input {
    String getId();

    Set<String> getOutputs();

    // TODO Check if needed and for what it was used.
    void setReaderFinished(ChunkReader chunkReader);

    public interface Factory<T extends Input, C extends Configuration> {
        T create(C configuration);
    }
}
