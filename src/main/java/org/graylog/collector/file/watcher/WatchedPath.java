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
