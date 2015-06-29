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
import org.graylog.collector.file.naming.FileNamingStrategy;
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

    private final Set<Path> monitoredFiles;
    private final FileNamingStrategy namingStrategy;
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

    public FileReaderService(Set<Path> monitoredFiles,
                             Charset charset,
                             FileNamingStrategy namingStrategy,
                             boolean followMode,
                             FileInput.InitialReadPosition initialReadPosition,
                             FileInput input,
                             MessageBuilder messageBuilder,
                             ContentSplitter contentSplitter,
                             Buffer buffer,
                             int readerBufferSize,
                             long readerInterval,
                             FileObserver fileObserver) {
        this.monitoredFiles = monitoredFiles;
        this.namingStrategy = namingStrategy;
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

        for (Path monitoredFile : monitoredFiles) {
            if (!monitoredFile.toFile().exists()) {
                if (followMode) {
                    log.warn("File {} does not exist but will be followed once it will be created.", monitoredFile);
                } else {
                    final String msg = "File " + monitoredFile + " does not exist and follow mode is not enabled. Not waiting for file to appear.";
                    log.error(msg);
                    notifyFailed(new IllegalStateException(msg));
                    return;
                }
            }

            chunkProcessor.addFileConfig(monitoredFile, "source", false);

            try {
                fileObserver.observePath(new FsChangeListener(monitoredFile), monitoredFile, namingStrategy);

                // for a previously existing file, we would not get a watcher callback, so we initialize the chunkreader here
                if (monitoredFile.toFile().exists()) {
                    chunkReaderScheduler.followFile(monitoredFile);
                }
            } catch (IOException e) {
                log.error("Unable to monitor directory for file " + monitoredFile, e);
                notifyFailed(e);
            }
        }

        chunkProcessor.startAsync();
        chunkProcessor.awaitRunning();
        fileObserver.startAsync();
        fileObserver.awaitRunning();

        notifyStarted();
    }

    @Override
    protected void doStop() {
        fileObserver.stopAsync();
        fileObserver.awaitTerminated();
        chunkProcessor.stopAsync();
        chunkProcessor.awaitTerminated();
        notifyStopped();
    }

    public class FsChangeListener implements FileObserver.Listener {
        private final Path monitoredFile;

        public FsChangeListener(Path monitoredFile) {
            this.monitoredFile = monitoredFile;
        }

        @Override
        public void pathCreated(Path path) {
            if (path.equals(monitoredFile)) {
                // a file with the same name as the one we should be monitoring has been created, start reading it
                if (chunkReaderScheduler.isFollowingFile(path)) {
                    chunkReaderScheduler.cancelFile(path);
                }
                try {
                    if (monitoredFile.toFile().exists()) {
                        chunkReaderScheduler.followFile(monitoredFile);
                    }
                } catch (IOException e) {
                    log.error("Cannot read newly created file " + monitoredFile, e);
                }
            } else {
                // the file is part of the logrotate 'family'
                // TODO make sure we have read everything from the rotated file (copytruncate will copy the original file)
            }
        }

        @Override
        public void pathRemoved(Path path) {
            if (path.equals(monitoredFile)) {
                // TODO someone removed our file, stop trying to read from it (reads might already have produced errors)
            } else {
                // TODO logrotate removed some file, check if we have read all data in it already, else send a warning that we
                // might have lost data
            }
        }

        @Override
        public void pathModified(Path path) {
            if (!chunkReaderScheduler.isFollowingFile(path) && path.equals(monitoredFile) && monitoredFile.toFile().exists()) {
                // Start following the modified file now. If we did not follow it before, there might have been an
                // error regarding permissions or something similar.
                try {
                    log.trace("Start following modified file {}", monitoredFile);
                    chunkReaderScheduler.followFile(monitoredFile);
                } catch (IOException e) {
                    log.error("Cannot read modified file " + monitoredFile, e);
                }
            }
        }

        @Override
        public void cannotObservePath(Path path) {
            // TODO directory was removed, can't observe anymore
        }
    }
}
