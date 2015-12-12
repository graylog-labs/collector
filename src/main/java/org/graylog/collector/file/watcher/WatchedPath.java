package org.graylog.collector.file.watcher;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;

import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.util.Set;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class WatchedPath {
    private final Path path;
    private final Set<PathEventListener> listeners = Sets.newConcurrentHashSet();

    public WatchedPath(Path path) {
        this.path = path;
    }

    public Path getPath() {
        return path;
    }

    public void addListener(Set<PathEventListener> listeners) {
        this.listeners.addAll(listeners);
    }

    public Set<PathEventListener> getListeners() {
        return listeners;
    }

    public void dispatchEvent(WatchEvent.Kind<Path> event, Path path) {
        for (PathEventListener listener : listeners) {
            if (event == ENTRY_CREATE) {
                listener.pathCreated(path);
            }
            if (event == ENTRY_DELETE) {
                listener.pathRemoved(path);
            }
            if (event == ENTRY_MODIFY) {
                listener.pathModified(path);
            }
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("path", path)
                .add("listenerCount", listeners.size())
                .toString();
    }
}
