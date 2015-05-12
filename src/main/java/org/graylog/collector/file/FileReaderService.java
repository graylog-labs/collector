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
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog.collector.MessageBuilder;
import org.graylog.collector.buffer.Buffer;
import org.graylog.collector.file.naming.FileNamingStrategy;
import org.graylog.collector.file.splitters.ContentSplitter;
import org.graylog.collector.inputs.file.FileInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class FileReaderService extends AbstractService {
    private static final Logger log = LoggerFactory.getLogger(FileReaderService.class);

    private final Path monitoredFile;
    private final FileNamingStrategy namingStrategy;
    private final boolean followMode;
    private final FileInput.InitialReadPosition initialReadPosition;
    private final FileInput input;
    private final MessageBuilder messageBuilder;
    private final ContentSplitter contentSplitter;
    private final Buffer buffer;
    private final Charset charset;
    private FileObserver fileObserver;
    private ChunkReader chunkReader;
    private ArrayBlockingQueue<FileChunk> chunkQueue;
    private ScheduledExecutorService scheduler;

    private FileObserver.Listener changeListener;
    private ChunkProcessor chunkProcessor;
    private ScheduledFuture<?> chunkReaderFuture;

    public FileReaderService(Path monitoredFile,
                             Charset charset,
                             FileNamingStrategy namingStrategy,
                             boolean followMode,
                             FileInput.InitialReadPosition initialReadPosition,
                             FileInput input,
                             MessageBuilder messageBuilder,
                             ContentSplitter contentSplitter,
                             Buffer buffer) {
        // TODO needs to be an absolute path because otherwise the FileObserver does weird things. Investigate what's wrong with it.
        this.monitoredFile = monitoredFile.toAbsolutePath();
        this.namingStrategy = namingStrategy;
        this.followMode = followMode;
        this.initialReadPosition = initialReadPosition;
        this.input = input;
        this.messageBuilder = messageBuilder;
        this.contentSplitter = contentSplitter;
        this.buffer = buffer;
        this.charset = charset;
        scheduler = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setDaemon(false)
                        .setNameFormat("fileinput-" + input.getId() + "-thread-%d")
                        .build());
        chunkQueue = Queues.newArrayBlockingQueue(2);
        changeListener = new FsChangeListener();
    }


    @Override
    protected void doStart() {
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
        chunkProcessor = new ChunkProcessor(buffer, messageBuilder, chunkQueue, contentSplitter, charset);
        chunkProcessor.addFileConfig(monitoredFile, "source", false);
        chunkProcessor.startAsync();
        chunkProcessor.awaitRunning();
        fileObserver = new FileObserver();
        try {
            fileObserver.observePath(changeListener, monitoredFile, namingStrategy);
            fileObserver.startAsync();
            fileObserver.awaitRunning();
            // for a previously existing file, we would not get a watcher callback, so we initialize the chunkreader here
            if (monitoredFile.toFile().exists()) {
                followFile();
            }
        } catch (Exception e) {
            log.error("Unable to monitor directory for file " + monitoredFile, e);
            notifyFailed(e);
        }
        notifyStarted();
    }

    private void followFile() throws IOException {
        AsynchronousFileChannel fileChannel = AsynchronousFileChannel.open(monitoredFile, StandardOpenOption.READ);

        chunkReader = new ChunkReader(input,
                monitoredFile,
                fileChannel,
                chunkQueue,
                1024,
                followMode,
                initialReadPosition);

        chunkReaderFuture = scheduler.scheduleAtFixedRate(chunkReader, 0, 250, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void doStop() {
        fileObserver.stopAsync();
        fileObserver.awaitTerminated();
        chunkProcessor.stopAsync();
        chunkProcessor.awaitTerminated();
        notifyStopped();
    }

    // default visibility for tests
    public FileObserver.Listener getChangeListener() {
        return changeListener;
    }

    // default visibility for tests
    public void setChangeListener(FileObserver.Listener changeListener) {
        this.changeListener = changeListener;
    }

    public class FsChangeListener implements FileObserver.Listener {

        @Override
        public void pathCreated(Path path) {
            if (path.equals(monitoredFile)) {
                // a file with the same name as the one we should be monitoring has been created, start reading it
                // TODO if there is a chunkreader already, check the fileKey of the underlying file
                if (chunkReaderFuture != null) {
                    chunkReaderFuture.cancel(false);
                    chunkReaderFuture = null;
                    chunkReader = null;
                }
                try {
                    if (monitoredFile.toFile().exists()) {
                        followFile();
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
            // TODO do we need this?
        }

        @Override
        public void cannotObservePath(Path path) {
            // TODO directory was removed, can't observe anymore
        }
    }
}
