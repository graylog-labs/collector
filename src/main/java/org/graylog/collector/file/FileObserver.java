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
package org.graylog.collector.file;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.sun.nio.file.SensitivityWatchEventModifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

public class FileObserver extends AbstractExecutionThreadService {
    private static final Logger log = LoggerFactory.getLogger(FileObserver.class);

    private final WatchService watcher;
    private final ConcurrentMap<WatchKey, Set<WatchPath>> keys = Maps.newConcurrentMap();

    private class WatchPath {
        private final PathSet pathSet;
        private final Listener listener;


        public WatchPath(PathSet pathSet, Listener listener) {
            this.pathSet = pathSet;
            this.listener = listener;
        }

        public Path getPath() {
            return pathSet.getRootPath();
        }

        public boolean matches(Path path) {
            return pathSet.isInSet(path);
        }

        public void dispatchEvent(WatchEvent.Kind<Path> event, Path path) {
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

        public void dispatchInvalid() {
            listener.cannotObservePath(pathSet.getRootPath());
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("pathSet", pathSet.getPattern()).toString();
        }
    }

    @Inject
    public FileObserver(WatchService watchService) {
        watcher = watchService;
    }

    public void observePathSet(PathSet pathSet, Listener listener) throws IOException {
        Preconditions.checkNotNull(listener);
        Preconditions.checkNotNull(pathSet);

        final Path rootPath = pathSet.getRootPath();

        log.debug("Watching directory {} for changes matching: {}", rootPath, pathSet.getPattern());

        final WatchKey key = rootPath.register(watcher, new WatchEvent.Kind[]{ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY},
                SensitivityWatchEventModifier.HIGH);

        keys.putIfAbsent(key, Sets.<WatchPath>newConcurrentHashSet());
        keys.get(key).add(new WatchPath(pathSet, listener));
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
            if (key == null || !key.isValid()) {
                // nothing to do
                continue;
            }

            final Set<WatchPath> watchPaths = keys.get(key);
            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.kind() == OVERFLOW) {
                    log.error("Too many changes occurred when watching files, we lost updates.");
                    continue;
                }
                @SuppressWarnings("unchecked")
                final WatchEvent<Path> ev = (WatchEvent<Path>) event;

                for (WatchPath watchPath : watchPaths) {
                    final Path path = watchPath.getPath().resolve(ev.context());

                    if (watchPath.matches(path)) {
                        if (log.isTraceEnabled()) {
                            log.trace("Dispatching event {} for path {} received to {}", ev.kind(), path, watchPath);
                        }
                        watchPath.dispatchEvent(ev.kind(), path);
                    } else {
                        // this file path does not belong to the set of files we are interested in
                        if (log.isTraceEnabled()) {
                            log.trace("Ignoring change {} to path {} - No match in path set {}", ev.kind().name(), path.toString(), watchPath);
                        }
                    }
                }
            }

            final boolean valid = key.reset();
            if (!valid) {
                for (WatchPath watchPath : watchPaths) {
                    watchPath.dispatchInvalid();
                }
            }
        }

        watcher.close();
    }

    public interface Listener {
        void pathCreated(Path path);

        void pathRemoved(Path path);

        void pathModified(Path path);

        void cannotObservePath(Path path);
    }
}
