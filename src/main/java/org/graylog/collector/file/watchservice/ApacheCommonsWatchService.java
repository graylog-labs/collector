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
package org.graylog.collector.file.watchservice;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.io.monitor.FileAlterationListener;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.Watchable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class ApacheCommonsWatchService implements CollectorWatchService {
    private static final Logger log = LoggerFactory.getLogger(ApacheCommonsWatchService.class);

    private static final ThreadFactory THREAD_FACTORY = new ThreadFactoryBuilder()
            .setDaemon(true)
            .setNameFormat("commons-io-watch-service-%d")
            .build();

    private final FileAlterationMonitor monitor;
    private final int eventQueueSize;
    private final BlockingQueue<Key> queue;

    public ApacheCommonsWatchService(long interval, int eventQueueSize) {
        // TODO Make values configurable.
        this.monitor = new FileAlterationMonitor(interval);
        this.queue = new LinkedBlockingQueue<>();
        this.eventQueueSize = eventQueueSize;

        monitor.setThreadFactory(THREAD_FACTORY);
        try {
            monitor.start();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public WatchKey register(Path path, WatchEvent.Kind<?>[] events, WatchEvent.Modifier... modifiers) throws IOException {
        final List<WatchEvent.Kind<?>> subscribedEvents = Arrays.asList(events);
        final FileAlterationObserver observer = new FileAlterationObserver(path.toFile());
        final Key watchKey = new Key(path, eventQueueSize, this);

        observer.addListener(new FileAlterationListener() {
            @Override
            public void onStart(FileAlterationObserver observer) {

            }

            @Override
            public void onDirectoryCreate(File directory) {

            }

            @Override
            public void onDirectoryChange(File directory) {

            }

            @Override
            public void onDirectoryDelete(File directory) {

            }

            @Override
            public void onFileCreate(File file) {
                if (subscribedEvents.contains(StandardWatchEventKinds.ENTRY_CREATE)) {
                    watchKey.addEvent(new Event<>(StandardWatchEventKinds.ENTRY_CREATE, 1, file.toPath()));
                }
            }

            @Override
            public void onFileChange(File file) {
                if (subscribedEvents.contains(StandardWatchEventKinds.ENTRY_MODIFY)) {
                    watchKey.addEvent(new Event<>(StandardWatchEventKinds.ENTRY_MODIFY, 1, file.toPath()));
                }
            }

            @Override
            public void onFileDelete(File file) {
                if (subscribedEvents.contains(StandardWatchEventKinds.ENTRY_DELETE)) {
                    watchKey.addEvent(new Event<>(StandardWatchEventKinds.ENTRY_DELETE, 1, file.toPath()));
                }
            }

            @Override
            public void onStop(FileAlterationObserver observer) {

            }
        });

        monitor.addObserver(observer);

        return watchKey;
    }

    private void enqueue(Key key) {
        queue.add(key);
    }

    @Override
    public WatchKey poll(long timeout, TimeUnit unit) throws InterruptedException {
        return queue.poll(timeout, unit);
    }

    @Override
    public void close() throws IOException {
        try {
            monitor.stop();
        } catch (Exception e) {
            log.error("Error stopping watch service", e);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("eventQueueSize", eventQueueSize)
                .add("queue", queue)
                .toString();
    }

    public static class Event<T> implements WatchEvent<T> {
        private final Kind<T> kind;
        private final AtomicInteger count;

        @Nullable
        private final T context;

        public Event(Kind<T> kind, int count, @Nullable T context) {
            this.kind = checkNotNull(kind);
            checkArgument(count >= 0, "count (%s) must be non-negative", count);
            this.count = new AtomicInteger(count);
            this.context = context;
        }

        @Override
        public Kind<T> kind() {
            return kind;
        }

        @Override
        public int count() {
            return count.get();
        }

        public void increment() {
            count.incrementAndGet();
        }

        @Nullable
        @Override
        public T context() {
            return context;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof Event) {
                Event<?> other = (Event<?>) obj;
                return kind().equals(other.kind())
                        && Objects.equal(context(), other.context());
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(kind(), context());
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("kind", kind())
                    .add("count", count())
                    .add("context", context())
                    .toString();
        }
    }

    public static class Key implements WatchKey {
        private final Path path;
        private final int queueLimit;
        private final ApacheCommonsWatchService watchService;
        private final List<WatchEvent<?>> events;

        private volatile boolean enqueued = false;
        private volatile boolean valid = true;

        public Key(Path path, int queueLimit, ApacheCommonsWatchService watchService) {
            this.path = path;
            this.queueLimit = queueLimit;
            this.watchService = watchService;
            this.events = new ArrayList<>();
        }

        public void addEvent(WatchEvent<?> event) {
            synchronized (events) {
                final int size = events.size();
                final WatchEvent<?> previous = size > 0 ? events.get(size - 1) : null;

                if (previous != null && event.equals(previous)) {
                    if (log.isTraceEnabled()) {
                        log.trace("Repeated event, incrementing counter for {}", previous);
                    }
                    ((Event<?>) previous).increment();
                    return;
                }

                // Remove the oldest event if the events list is full.
                if (size > queueLimit) {
                    final WatchEvent<?> removed = events.remove(0);
                    log.warn("Events queue is full (max {}), removed oldest event: {} (increase \"file-watch-service.event-queue-size\" setting)", queueLimit, removed);
                }

                if (log.isTraceEnabled()) {
                    log.trace("Adding event: {}", event);
                }
                events.add(event);

                if (!enqueued) {
                    enqueued = true;
                    if (log.isTraceEnabled()) {
                        log.trace("Enqueuing key [{}] with watch service", this);
                    }
                    watchService.enqueue(this);
                }
            }
        }

        public boolean hasEvents() {
            synchronized (events) {
                return !events.isEmpty();
            }
        }

        @Override
        public boolean isValid() {
            return valid;
        }

        @Override
        public List<WatchEvent<?>> pollEvents() {
            synchronized (events) {
                final List<WatchEvent<?>> eventsCopy = new ArrayList<>(events.size());

                eventsCopy.addAll(events);
                events.clear();

                return eventsCopy;
            }
        }

        @Override
        public boolean reset() {
            if (enqueued && isValid()) {
                synchronized (events) {
                    if (events.isEmpty()) {
                        enqueued = false;
                    } else {
                        // There are events left so enqueue this object again.
                        watchService.enqueue(this);
                    }
                }
            }

            return true;
        }

        @Override
        public void cancel() {
            valid = false;
        }

        @Override
        public Watchable watchable() {
            return path;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Key key = (Key) o;

            return path.equals(key.path);
        }

        @Override
        public int hashCode() {
            return path.hashCode();
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("enqueued", enqueued)
                    .add("path", path)
                    .add("queueLimit", queueLimit)
                    .add("watchService", watchService)
                    .add("events", events)
                    .add("valid", valid)
                    .toString();
        }
    }
}
