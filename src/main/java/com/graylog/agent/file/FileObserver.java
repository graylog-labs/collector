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
package com.graylog.agent.file;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.graylog.agent.file.naming.FileNamingStrategy;
import com.sun.nio.file.SensitivityWatchEventModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public class FileObserver extends AbstractExecutionThreadService {
    private static final Logger log = LoggerFactory.getLogger(FileObserver.class);

    private final WatchService watcher;
    private final Map<WatchKey, WatchEntry> keys;

    private class WatchEntry {
        public Path path;
        public FileNamingStrategy namingStrategy;
        public Listener listener;

        public WatchEntry(Path path, FileNamingStrategy namingStrategy, Listener listener) {
            this.namingStrategy = namingStrategy;
            this.path = path;
            this.listener = listener;
        }
    }

    public FileObserver() {
        WatchService tmp;
        try {
            tmp = FileSystems.getDefault().newWatchService();
        } catch (IOException e) {
            tmp = null;
            // shouldn't happen
        }
        watcher = tmp;
        keys = Maps.newConcurrentMap();
    }

    public void observePath(Listener listener, Path path, FileNamingStrategy namingStrategy) throws IOException {
        Preconditions.checkNotNull(listener);
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(namingStrategy);

        Path directory = path;
        if (!Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)) {
            directory = path.getParent();
        }
        final WatchKey key = directory.register(watcher, new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY},
                SensitivityWatchEventModifier.HIGH);
        log.debug("Watching directory {} for file {}", directory, path);
        keys.put(key, new WatchEntry(path, namingStrategy, listener));
    }

    @Override
    protected void run() throws Exception {
        while (isRunning()) {
            WatchKey key;
            try {
                key = watcher.poll(500, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                return;
            }
            if (key == null) {
                // nothing to do
                continue;
            }

            final WatchEntry watchEntry = keys.get(key);
            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.kind() == OVERFLOW) {
                    log.error("Too many changes occurred when watching files, we lost updates.");
                    continue;
                }
                @SuppressWarnings("unchecked")
                final WatchEvent<Path> ev = (WatchEvent<Path>) event;
                Path path = ev.context();
                path = path.resolve(watchEntry.path);
                log.debug("Event {} for path {} received", ev.kind(), path);

                if (!watchEntry.namingStrategy.pathMatches(path)) {
                    // this file path does not belong to the set of files we are interested in
                    log.trace("Ignoring change [] to path []. Does not fit naming scheme.", ev.kind().name(), path.toString());
                }
                if (ev.kind() == ENTRY_CREATE) {
                    watchEntry.listener.pathCreated(path);
                }
                if (ev.kind() == ENTRY_DELETE) {
                    watchEntry.listener.pathRemoved(path);
                }
                if (ev.kind() == ENTRY_MODIFY) {
                    watchEntry.listener.pathModified(path);
                }
            }

            final boolean valid = key.reset();
            if (!valid) {
                watchEntry.listener.cannotObservePath(watchEntry.path);
            }
        }
    }

    public interface Listener {
        void pathCreated(Path path);

        void pathRemoved(Path path);

        void pathModified(Path path);

        void cannotObservePath(Path path);
    }
}
