package org.graylog.collector.file;

import com.google.inject.Scopes;
import org.graylog.collector.guice.CollectorModule;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.WatchService;

public class FileModule extends CollectorModule {
    @Override
    protected void configure() {
        try {
            bind(WatchService.class).toInstance(FileSystems.getDefault().newWatchService());
        } catch (IOException e) {
            throw new RuntimeException("Unable to create WatchService");
        }

        bind(FileObserver.class).in(Scopes.SINGLETON);
        registerService(FileObserver.class);
    }
}
