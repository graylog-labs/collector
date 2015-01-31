package com.graylog.agent.file.compat;

import com.graylog.agent.file.ChunkReader;

public interface FileInput extends MessageInput {
    String getId();

    void setReaderFinished(ChunkReader chunkReader);

    public enum InitialReadPosition {
        END
    }
}
