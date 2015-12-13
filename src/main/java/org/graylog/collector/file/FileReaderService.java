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

import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.AbstractService;
import org.graylog.collector.MessageBuilder;
import org.graylog.collector.buffer.Buffer;
import org.graylog.collector.file.naming.FileNamingStrategy;
import org.graylog.collector.file.splitters.ContentSplitter;
import org.graylog.collector.file.watcher.PathEventListener;
import org.graylog.collector.file.watcher.PathWatcher;
import org.graylog.collector.inputs.file.FileInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;

public class FileReaderService extends AbstractService {
    private static final Logger log = LoggerFactory.getLogger(FileReaderService.class);

    private final Path path;
    private final FileNamingStrategy fileNamingStrategy;
    private final FileInput.InitialReadPosition initialReadPosition;
    private final FileInput input;
    private final MessageBuilder messageBuilder;
    private final ContentSplitter contentSplitter;
    private final Buffer buffer;
    private final Charset charset;
    private final int readerBufferSize;
    private final long readerInterval;
    private final PathWatcher pathWatcher;
    private ArrayBlockingQueue<FileChunk> chunkQueue;

    private ChunkProcessor chunkProcessor;
    private ChunkReaderScheduler chunkReaderScheduler;

    public FileReaderService(Path path,
                             FileNamingStrategy fileNamingStrategy,
                             Charset charset,
                             FileInput.InitialReadPosition initialReadPosition,
                             FileInput input,
                             MessageBuilder messageBuilder,
                             ContentSplitter contentSplitter,
                             Buffer buffer,
                             int readerBufferSize,
                             long readerInterval,
                             PathWatcher pathWatcher) {
        this.path = path;
        this.fileNamingStrategy = fileNamingStrategy;
        this.initialReadPosition = initialReadPosition;
        this.input = input;
        this.messageBuilder = messageBuilder;
        this.contentSplitter = contentSplitter;
        this.buffer = buffer;
        this.charset = charset;
        this.readerBufferSize = readerBufferSize;
        this.readerInterval = readerInterval;
        this.pathWatcher = pathWatcher;
        chunkQueue = Queues.newArrayBlockingQueue(2);
    }


    @Override
    protected void doStart() {
        chunkReaderScheduler = new ChunkReaderScheduler(input, chunkQueue, readerBufferSize, readerInterval, initialReadPosition);

        chunkProcessor = new ChunkProcessor(buffer, messageBuilder, chunkQueue, contentSplitter, charset);
        chunkProcessor.startAsync().awaitRunning();

        try {
            pathWatcher.register(path, Sets.<PathEventListener>newHashSet(new EventListener()));
        } catch (IOException e) {
            log.error("Unable to monitor directory: {}", path);
            notifyFailed(e);
            return;
        }

        notifyStarted();
    }

    @Override
    protected void doStop() {
        chunkProcessor.stopAsync();
        chunkProcessor.awaitTerminated();
        notifyStopped();
    }

    private class EventListener implements PathEventListener {
        @Override
        public void pathCreated(Path path) {
            if (Files.isDirectory(path)) {
                log.info("Path {} is a directory, skipping.", path);
                return;
            }
            if (!fileNamingStrategy.pathMatches(path)) {
                log.info("File does not match pattern: {}", path);
                return;
            }
            // a file with the same name as the one we should be monitoring has been created, start reading it
            if (chunkReaderScheduler.isFollowingFile(path)) {
                log.info("Cancel existing follow for {}", path);
                chunkReaderScheduler.cancelFile(path);
            }
            try {
                if (path.toFile().exists()) {
                    log.info("Follow created file {}", path);
                    chunkReaderScheduler.followFile(path);
                }
            } catch (IOException e) {
                log.error("Cannot read newly created file " + path, e);
            }
        }

        @Override
        public void pathModified(Path path) {
            if (Files.isDirectory(path)) {
                log.info("Path {} is a directory, skipping.", path);
                return;
            }
            if (!fileNamingStrategy.pathMatches(path)) {
                log.info("File does not match pattern: {}", path);
                return;
            }
            if (!chunkReaderScheduler.isFollowingFile(path) && path.toFile().exists()) {
                // Start following the modified file now. If we did not follow it before, there might have been an
                // error regarding permissions or something similar.
                try {
                    log.info("Start following modified file {}", path);
                    chunkReaderScheduler.followFile(path);
                } catch (IOException e) {
                    log.error("Cannot read modified file " + path, e);
                }
            }
        }

        @Override
        public void pathRemoved(Path path) {
            // File got removed, stop following it.
            log.info("Cancel file {}", path);
            chunkReaderScheduler.cancelFile(path);
        }
    }
}
