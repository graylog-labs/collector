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

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog.collector.inputs.Input;
import org.graylog.collector.inputs.file.FileInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ChunkReaderScheduler {
    private static final Logger log = LoggerFactory.getLogger(ChunkReaderScheduler.class);

    private final Input input;
    private final int readerBufferSize;
    private final long readerInterval;
    private final FileInput.InitialReadPosition initialReadPosition;
    private final ArrayBlockingQueue<FileChunk> chunkQueue;
    private final ConcurrentMap<Path, ChunkReaderTask> chunkReaderTasks = Maps.newConcurrentMap();
    private final ScheduledExecutorService scheduler;

    public ChunkReaderScheduler(final Input input,
                                final ArrayBlockingQueue<FileChunk> chunkQueue,
                                final int readerBufferSize,
                                final long readerInterval,
                                final FileInput.InitialReadPosition initialReadPosition) {
        this.input = input;
        this.chunkQueue = chunkQueue;
        this.readerBufferSize = readerBufferSize;
        this.readerInterval = readerInterval;
        this.initialReadPosition = initialReadPosition;

        // TODO Make the thread size configurable.
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setDaemon(false)
                        .setNameFormat("chunkreader-scheduler-thread-" + input.getId() + "-%d")
                        .build());
    }

    public boolean isFollowingFile(Path file) {
        // TODO if there is a chunkreader already, check the fileKey of the underlying file
        return chunkReaderTasks.containsKey(file);
    }

    public void followFile(Path file) throws IOException {
        followFile(file, initialReadPosition);
    }

    public void followFile(Path file, FileInput.InitialReadPosition customInitialReadPosition) throws IOException {
        synchronized (this) {
            if (isFollowingFile(file)) {
                log.debug("Not following file {} because it's already followed.", file);
                return;
            }

            log.debug("Following file {}", file);

            final AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(file, StandardOpenOption.READ);
            final ChunkReader chunkReader = new ChunkReader(input, file, fileChannel, chunkQueue, readerBufferSize, customInitialReadPosition, this);
            final ScheduledFuture<?> chunkReaderFuture = scheduler.scheduleAtFixedRate(chunkReader, 0, readerInterval, TimeUnit.MILLISECONDS);

            chunkReaderTasks.putIfAbsent(file, new ChunkReaderTask(chunkReaderFuture, fileChannel));
        }
    }

    public void restartFile(Path file) {
        try {
            log.debug("Restart file {}", file);
            // Synchronize to make this atomic and to avoid other events trigger a follow.
            synchronized (this) {
                cancelFile(file);
                followFile(file, FileInput.InitialReadPosition.START);
            }
        } catch (IOException e) {
            log.error("Error restarting file", e);
        }
    }

    public void cancelFile(Path file) {
        final ChunkReaderTask task = chunkReaderTasks.remove(file);

        if (task != null) {
            log.debug("Cancel file {}", file);
            try {
                task.cancel();
            } catch (IOException e) {
                log.error("Unable to stop chunk reader task", e);
            }
        }
    }

    private static class ChunkReaderTask {
        private final ScheduledFuture<?> chunkReaderFuture;
        private final AsynchronousFileChannel fileChannel;

        public ChunkReaderTask(ScheduledFuture<?> chunkReaderFuture, AsynchronousFileChannel fileChannel) {
            this.chunkReaderFuture = chunkReaderFuture;
            this.fileChannel = fileChannel;
        }

        public void cancel() throws IOException {
            chunkReaderFuture.cancel(false);
            fileChannel.close();
        }
    }
}
