package org.graylog.collector.file.watcher;

import java.nio.file.Path;

public interface PathEventListener {
    void pathCreated(Path path);

    void pathModified(Path path);

    void pathRemoved(Path path);
}
