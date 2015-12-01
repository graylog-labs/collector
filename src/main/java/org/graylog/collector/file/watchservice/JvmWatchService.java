package org.graylog.collector.file.watchservice;

import com.google.common.base.MoreObjects;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.concurrent.TimeUnit;

public class JvmWatchService implements CollectorWatchService {
    private final WatchService watchService;

    @Inject
    public JvmWatchService(WatchService watchService) {
        this.watchService = watchService;
    }

    @Override
    public WatchKey register(Path path, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        return path.register(watchService, events, modifiers);
    }

    @Override
    public WatchKey poll(long timeout, TimeUnit unit) throws InterruptedException {
        return watchService.poll(timeout, unit);
    }

    @Override
    public void close() throws IOException {
        watchService.close();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("watchService", watchService.getClass())
                .toString();
    }
}
