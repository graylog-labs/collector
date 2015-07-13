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
import com.google.common.util.concurrent.AbstractService;
import org.graylog.collector.MessageBuilder;
import org.graylog.collector.buffer.Buffer;
import org.graylog.collector.file.splitters.ContentSplitter;
import org.graylog.collector.inputs.file.FileInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

public class FileReaderService extends AbstractService {
    private static final Logger log = LoggerFactory.getLogger(FileReaderService.class);

    private final PathSet pathSet;
    private final boolean followMode;
    private final FileInput.InitialReadPosition initialReadPosition;
    private final FileInput input;
    private final MessageBuilder messageBuilder;
    private final ContentSplitter contentSplitter;
    private final Buffer buffer;
    private final Charset charset;
    private final int readerBufferSize;
    private final long readerInterval;
    private final FileObserver fileObserver;
    private ArrayBlockingQueue<FileChunk> chunkQueue;

    private ChunkProcessor chunkProcessor;
    private ChunkReaderScheduler chunkReaderScheduler;

    public FileReaderService(PathSet pathSet,
                             Charset charset,
                             boolean followMode,
                             FileInput.InitialReadPosition initialReadPosition,
                             FileInput input,
                             MessageBuilder messageBuilder,
                             ContentSplitter contentSplitter,
                             Buffer buffer,
                             int readerBufferSize,
                             long readerInterval,
                             FileObserver fileObserver) {
        this.pathSet = pathSet;
        this.followMode = followMode;
        this.initialReadPosition = initialReadPosition;
        this.input = input;
        this.messageBuilder = messageBuilder;
        this.contentSplitter = contentSplitter;
        this.buffer = buffer;
        this.charset = charset;
        this.readerBufferSize = readerBufferSize;
        this.readerInterval = readerInterval;
        this.fileObserver = fileObserver;
        chunkQueue = Queues.newArrayBlockingQueue(2);
    }


    @Override
    protected void doStart() {
        chunkReaderScheduler = new ChunkReaderScheduler(input, chunkQueue, readerBufferSize, readerInterval, followMode, initialReadPosition);
        chunkProcessor = new ChunkProcessor(buffer, messageBuilder, chunkQueue, contentSplitter, charset);

        try {
            fileObserver.observePathSet(pathSet, new FsChangeListener());
        } catch (IOException e) {
            log.error("Unable to monitor directory: {}", pathSet.getRootPath());
            notifyFailed(e);
            return;
        }

        final Set<Path> paths;
        try {
            paths = pathSet.getPaths();
        } catch (IOException e) {
            log.error("Unable to compute paths", e);
            notifyFailed(e);
            return;
        }

        if (paths.isEmpty()) {
            log.info("Configured files for input \"{}\" do not exist yet. They will be followed once they are created.",
                    input.getId());
        }

        for (Path path : paths) {
            if (!path.toFile().exists()) {
                if (followMode) {
                    log.warn("File {} does not exist but will be followed once it will be created.", path);
                } else {
                    final String msg = "File " + path + " does not exist and follow mode is not enabled. Not waiting for file to appear.";
                    log.error(msg);
                    notifyFailed(new IllegalStateException(msg));
                    return;
                }
            }

            try {
                // for a previously existing file, we would not get a watcher callback, so we initialize the chunkreader here
                if (path.toFile().exists()) {
                    chunkReaderScheduler.followFile(path);
                }
            } catch (IOException e) {
                log.error("Unable to follow file: " + path, e);
                notifyFailed(e);
            }
        }

        chunkProcessor.startAsync();
        chunkProcessor.awaitRunning();

        notifyStarted();
    }

    @Override
    protected void doStop() {
        chunkProcessor.stopAsync();
        chunkProcessor.awaitTerminated();
        notifyStopped();
    }

    public class FsChangeListener implements FileObserver.Listener {
        @Override
        public void pathCreated(Path path) {
            // a file with the same name as the one we should be monitoring has been created, start reading it
            if (chunkReaderScheduler.isFollowingFile(path)) {
                chunkReaderScheduler.cancelFile(path);
            }
            try {
                if (path.toFile().exists()) {
                    chunkReaderScheduler.followFile(path);
                }
            } catch (IOException e) {
                log.error("Cannot read newly created file " + path, e);
            }
        }

        @Override
        public void pathRemoved(Path path) {
            // File got removed, stop following it.
            chunkReaderScheduler.cancelFile(path);
        }

        @Override
        public void pathModified(Path path) {
            if (!chunkReaderScheduler.isFollowingFile(path) && path.toFile().exists()) {
                // Start following the modified file now. If we did not follow it before, there might have been an
                // error regarding permissions or something similar.
                try {
                    log.trace("Start following modified file {}", path);
                    chunkReaderScheduler.followFile(path);
                } catch (IOException e) {
                    log.error("Cannot read modified file " + path, e);
                }
            }
        }

        @Override
        public void cannotObservePath(Path path) {
            // TODO directory was removed, can't observe anymore
        }
    }
}
