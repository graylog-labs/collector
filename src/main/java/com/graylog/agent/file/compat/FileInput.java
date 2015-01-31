package com.graylog.agent.file.compat;

import com.graylog.agent.file.ChunkReader;

public class FileInput implements MessageInput {
    public String getId() {
        return null;
    }

    public void setReaderFinished(ChunkReader chunkReader) {

    }

    @Override
    public void initialize(Configuration config) {

    }

    @Override
    public void launch(Buffer mockBuffer) {

    }

    public enum InitialReadPosition {
        START, END
    }
}
