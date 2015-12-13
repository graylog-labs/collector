/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.collector.file.watcher;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.sun.nio.file.SensitivityWatchEventModifier;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public class PathWatcher extends AbstractExecutionThreadService {
    private static final Logger log = LoggerFactory.getLogger(PathWatcher.class);

    private final WatchService watcher;
    private final Duration eventPollTimeout;
    private final ConcurrentMap<WatchKey, WatchedPath> keys;

    @Inject
    public PathWatcher(WatchService watcher, @Named("watch-service-event-poll-timeout") Duration eventPollTimeout) {
        this.watcher = watcher;
        this.eventPollTimeout = eventPollTimeout;
        this.keys = new ConcurrentHashMap<>();
    }

    private Optional<WatchedPath> registerPath(final Path dir, final Set<PathEventListener> listeners) throws IOException {
        final WatchKey key = dir.register(watcher, new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY},
                SensitivityWatchEventModifier.HIGH);

        synchronized (keys) {
            final WatchedPath prev = keys.putIfAbsent(key, new WatchedPath(dir));
            keys.get(key).addListener(listeners);

            if (prev == null) {
                log.info("Register path: {}", dir);
                return Optional.of(keys.get(key));
            } else {
                log.info("Path already registered: {}", dir);
                return Optional.absent();
            }
        }
    }

    /**
     * Register the given directory, and all its sub-directories, with the WatchService.
     */
    public void register(final Path start, final Set<PathEventListener> listeners) throws IOException {
        checkNotNull(start);
        checkNotNull(listeners);
        Files.walkFileTree(start, new SimpleFileVisitor<Path>() {
            public Optional<WatchedPath> currentPathWatch = Optional.absent();

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                log.error("Unable to read directory: {}", file);
                return FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (currentPathWatch.isPresent()) {
                    currentPathWatch.get().dispatchEvent(ENTRY_CREATE, file);
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                // Skip /proc because it can throw some permission errors we cannot check for.
                if ("/proc".equals(dir.toString())) {
                    return FileVisitResult.SKIP_SUBTREE;
                }
                this.currentPathWatch = registerPath(dir, listeners);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public void unregister(final Path path) {
        for (final Map.Entry<WatchKey, WatchedPath> entry : keys.entrySet()) {
            if (entry.getValue().getPath().equals(path)) {
                unregister(entry.getKey());
            }
        }
    }

    private void unregister(final WatchKey key) {
        final WatchedPath remove = keys.remove(key);
        key.cancel();
        log.info("Removing registered directory: {}", remove.getPath());
    }

    private void processEvents() {
        while (isRunning()) {
            final WatchKey key;
            try {
                key = watcher.poll(eventPollTimeout.getMillis(), TimeUnit.MILLISECONDS);
            } catch (InterruptedException x) {
                return;
            }

            if (key == null) {
                // nothing to do
                continue;
            }

            final WatchedPath watchedPath = keys.get(key);

            if (watchedPath == null) {
                log.error("Path not registered: {}", key.watchable());
                key.cancel();
                continue;
            }

            for (final WatchEvent<?> event : key.pollEvents()) {
                final WatchEvent.Kind kind = event.kind();

                if (kind == OVERFLOW) {
                    log.error("Too many changes occurred when watching files, we lost updates.");
                    continue;
                }

                // Context for directory entry event is the file name of entry
                @SuppressWarnings("unchecked")
                final WatchEvent<Path> ev = (WatchEvent<Path>) event;
                final Path name = ev.context();
                final Path child = watchedPath.getPath().resolve(name);

                watchedPath.dispatchEvent(ev.kind(), child);

                // If the watch is a directory, register it and its sub directories.
                if (kind == ENTRY_CREATE) {
                    try {
                        if (Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS)) {
                            register(child, watchedPath.getListeners());
                        }
                    } catch (IOException e) {
                        log.error("Unable to register subdirectory: " + child.toString(), e);
                    }
                }
            }

            // Reset the key and remove it from keys if the directory no longer accessible.
            final boolean valid = key.reset();
            if (!valid) {
                unregister(key);

                // all directories are inaccessible
                if (keys.isEmpty()) {
                    break;
                }
            }
        }
    }

    @Override
    protected void run() throws Exception {
        processEvents();
        watcher.close();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("eventPollTimeout", eventPollTimeout.toString())
                .add("watchedPaths", keys.values())
                .add("watcher", watcher)
                .toString();
    }
}
