package com.graylog.agent.inputs;

import com.graylog.agent.file.ChunkReader;

public interface Input {
    // TODO Check if needed and for what it was used.
    void setReaderFinished(ChunkReader chunkReader);
}
