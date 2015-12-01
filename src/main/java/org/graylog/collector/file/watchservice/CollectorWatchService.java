package org.graylog.collector.file.watchservice;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.util.concurrent.TimeUnit;

public interface CollectorWatchService {
    WatchKey register(Path path, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException;

    WatchKey poll(long timeout, TimeUnit unit) throws InterruptedException;

    void close() throws IOException;
}
