package com.graylog.agent.file;

import com.google.common.base.Preconditions;
import com.graylog.agent.inputs.Input;
import com.graylog.agent.inputs.file.FileInput;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChunkReader implements Runnable {
    private static final Logger log = LoggerFactory.getLogger(ChunkReader.class);

    private final Input fileInput;
    private final Path path;
    private final AsynchronousFileChannel fileChannel;
    private final BlockingQueue<FileChunk> chunks;
    private final int initialChunkSize;
    private final boolean followMode;
    private int lastReadSize = 0; // TODO for adaptive read sizing
    private long position = 0;
    private AtomicBoolean locked = new AtomicBoolean(false);
    private FileChunk queuedChunk;
    private long chunkId = 0;
    private Object fileKey;

    public ChunkReader(Input fileInput,
                       Path path,
                       AsynchronousFileChannel fileChannel,
                       BlockingQueue<FileChunk> chunks,
                       int initialChunkSize,
                       boolean followMode,
                       FileInput.InitialReadPosition initialReadPosition) {
        this.fileInput = fileInput;
        this.path = path;
        this.fileChannel = fileChannel;
        this.chunks = chunks;
        this.initialChunkSize = initialChunkSize;
        Preconditions.checkArgument(initialChunkSize > 0, "Chunk size must be positive");

        this.followMode = followMode;
        if (fileChannel.isOpen()) {
            try {
                final BasicFileAttributes attr = Files.readAttributes(path, BasicFileAttributes.class);
                fileKey = attr.fileKey();
                if (initialReadPosition == FileInput.InitialReadPosition.END) {
                    position = attr.size();
                }
            } catch (IOException e) {
                log.error("Cannot access file metadata", e);
            }
        }
    }

    @Override
    public void run() {
        final boolean lockAcquired = locked.compareAndSet(false, true);
        if (!lockAcquired) {
            // prevent reading two chunks the from the same file in two different threads, this would mean we'd have to
            // reassemble chunks
            log.debug("[{}] Could not acquire mutex, not running again.", path);
            return;
        }
        // the finally block must be executed!
        try {
            if (!fileChannel.isOpen()) {
                log.debug("[{}] File is not open, not performing more reads.", path);
                fileInput.setReaderFinished(this);
                // TODO signal read error
                throw new IllegalStateException("Don't want to run anymore");
            }
            if (queuedChunk != null) {
                // try to get rid of our queued buffer before reading another one.
                final boolean isQueued = chunks.offer(queuedChunk);
                if (isQueued) {
                    queuedChunk = null;
                    log.debug("[{}] Using queued chunk instead of reading another one.", path);
                } else {
                    log.debug("[{}] Queue still full, not reading more.", path);
                }
                return;
            }

            // If the size of the file is smaller than the read position, the file might have been truncated.
            if (fileChannel.size() < position) {
                log.trace("Reset read position");
                position = 0;
            }

            final ByteBuffer byteBuffer = ByteBuffer.allocateDirect(initialChunkSize);
            final Future<Integer> read = fileChannel.read(byteBuffer, position);
            final Integer bytesRead = read.get();
            log.trace("[{}] Read {} bytes from position {}", new Object[]{path, bytesRead, position});
            if (bytesRead != -1) {
                lastReadSize = bytesRead;
                position += bytesRead;
                byteBuffer.flip();
                final ByteBuf buffer = Unpooled.wrappedBuffer(byteBuffer);
                final FileChunk chunk = new FileChunk(path, buffer, ++chunkId);
                final boolean isQueued = chunks.offer(chunk);
                if (!isQueued) {
                    // the buffer could not be added to the queue, we'll buffer it in this chunkreader until we can get rid of it
                    queuedChunk = chunk;
                    log.debug("[{}] Unable to queue chunk, buffering it until next execution, not reading more chunks.",
                            path);
                    return;
                }
                log.debug("[{}] Queued chunk of {} bytes, chunk number {}", new Object[]{path, bytesRead, chunkId});
            } else {
                if (followMode) {
                    log.trace("[{}] Could not read any bytes from file, waiting for more", path);
                } else {
                    log.debug("[{}] Reached end of file in no-follow mode, closing file.", path);
                    try {
                        fileChannel.close();
                    } catch (IOException e) {
                        log.info("Unable to close file", e);
                    }
                    // signals the consumer of the queue that we won't produce any more chunks.
                    chunks.offer(FileChunk.finalChunk(path));
                }
            }

        } catch (InterruptedException e) {
            log.warn("[" + path + "] Interrupted read, should not happen", e);
            // TODO error handling
        } catch (ExecutionException e) {
            log.warn("[" + path + "] Read failed ", e);
            // TODO error handling
        } catch (IOException e) {
            log.warn("[" + path + "] I/O error", e);
            // TODO error handling
        } finally {
            locked.set(false);
        }
    }
}
