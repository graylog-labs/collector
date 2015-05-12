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

import com.google.common.base.Charsets;
import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog.collector.inputs.file.FileInput;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class ChunkReaderTest implements Thread.UncaughtExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ChunkReaderTest.class);

    @Test
    public void throttleChunkReading() throws IOException, InterruptedException {
        final Utils.LogFile logFile = new Utils.LogFile(100 * 1024, 400, 100);  // 100kb simulated log file, lines max 400 chars long, 100 chars deviation
        final Path tempFile = logFile.getPath();
        logFile.close();
        final ArrayBlockingQueue<FileChunk> chunkQueue = Queues.newArrayBlockingQueue(1);
        final AsynchronousFileChannel channel = AsynchronousFileChannel.open(tempFile, StandardOpenOption.READ);
        final CountingAsyncFileChannel spy = new CountingAsyncFileChannel(channel);

        final ChunkReader chunkReader = new ChunkReader(mock(FileInput.class), tempFile, spy, chunkQueue, 10 * 1024, false,
                FileInput.InitialReadPosition.START);

        final ScheduledExecutorService chunkReaderExecutor = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setDaemon(false)
                        .setNameFormat("file-chunk-reader-%d")
                        .setUncaughtExceptionHandler(this)
                        .build()
        );

        final Thread consumer = new Thread() {
            @Override
            public void run() {
                try {
                    while (null == chunkQueue.peek()) {
                        // spin until the first chunk appears, then block for longer than the executor schedules tasks
                    }
                    log.debug("Found first chunk");
                    // do nothing for a while, just make sure the chunkreader isn't trying to read from the channel in the meantime!
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) {
                }
            }
        };
        consumer.start();
        chunkReaderExecutor.scheduleAtFixedRate(chunkReader, 0, 250, TimeUnit.MILLISECONDS);
        consumer.join();

        // we can process one chunk at a time, so one read is queued, the second is buffered
        assertEquals("ChunkReader should perform two reads only", 2, spy.getReadCount());
        assertEquals("The queue should be full", 0, chunkQueue.remainingCapacity());
    }

    @Test
    public void readPositionEnd() throws IOException, InterruptedException {
        final Utils.LogFile logFile = new Utils.LogFile(100 * 1024, 400, 100);
        logFile.close();
        final ArrayBlockingQueue<FileChunk> chunkQueue = Queues.newArrayBlockingQueue(1);
        final AsynchronousFileChannel channel = AsynchronousFileChannel.open(logFile.getPath(), StandardOpenOption.READ);
        final CountingAsyncFileChannel spy = new CountingAsyncFileChannel(channel);

        final ChunkReader chunkReader = new ChunkReader(mock(FileInput.class), logFile.getPath(), spy, chunkQueue, 10 * 1024, true,
                FileInput.InitialReadPosition.END);

        final ScheduledExecutorService chunkReaderExecutor = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setDaemon(false)
                        .setNameFormat("file-chunk-reader-%d")
                        .setUncaughtExceptionHandler(this)
                        .build()
        );

        final Thread consumer = new Thread() {
            @Override
            public void run() {
                try {
                    final FileChunk chunk = chunkQueue.poll(2, TimeUnit.SECONDS);
                    assertNull("Reading from the end of the file must not produce a chunk for a non-changing file.", chunk);
                } catch (InterruptedException ignore) {
                }
            }
        };
        consumer.start();
        chunkReaderExecutor.scheduleAtFixedRate(chunkReader, 0, 250, TimeUnit.MILLISECONDS);
        consumer.join();

        // we can process one chunk at a time, so one read is queued, the second is buffered
        assertEquals("The e should be empty", 1, chunkQueue.remainingCapacity());
    }

    @Test
    public void positionEndReadNewlyAppendedData() throws IOException, InterruptedException {
        final Utils.LogFile logFile = new Utils.LogFile(100 * 1024, 400, 100);

        final ArrayBlockingQueue<FileChunk> chunkQueue = Queues.newArrayBlockingQueue(1);
        final AsynchronousFileChannel channel = AsynchronousFileChannel.open(logFile.getPath(), StandardOpenOption.READ);
        final CountingAsyncFileChannel spy = new CountingAsyncFileChannel(channel);

        final ChunkReader chunkReader = new ChunkReader(mock(FileInput.class), logFile.getPath(), spy, chunkQueue, 10 * 1024, true,
                FileInput.InitialReadPosition.END);

        final ScheduledExecutorService chunkReaderExecutor = Executors.newSingleThreadScheduledExecutor(
                new ThreadFactoryBuilder()
                        .setDaemon(false)
                        .setNameFormat("file-chunk-reader-%d")
                        .setUncaughtExceptionHandler(this)
                        .build()
        );

        final String appendedLine = "this is a single message";
        final CountDownLatch produceLatch = new CountDownLatch(1);
        final Thread producer = new Thread() {
            @Override
            public void run() {
                try {
                    produceLatch.await();
                    logFile.appendLine(appendedLine);
                    logFile.flush();
                } catch (InterruptedException e) {
                    assertNull("Shouldn't get interrupted.", e);
                } catch (IOException e) {
                    assertNull("Shouldn't fail to append line.", e);
                }
            }
        };
        producer.start();

        final Thread consumer = new Thread() {
            @Override
            public void run() {
                try {
                    final FileChunk chunk = chunkQueue.poll(2, TimeUnit.SECONDS);
                    assertNull("Reading from the end of the file must not produce a chunk for a non-changing file.", chunk);
                    produceLatch.countDown();
                    // this assumes that we will read the entire line we've written above.
                    // might be brittle and can break if we happen to read only part of the strings, but given the
                    // size of it it's unlikely. if this spuriously breaks, read chunks in a loop and buffer up.
                    final FileChunk nextChunk = chunkQueue.poll(2, TimeUnit.SECONDS);
                    assertNotNull("the next chunk should be filled", nextChunk);
                    assertEquals("line should match", appendedLine + "\n", nextChunk.getChunkBuffer().toString(Charsets.UTF_8));
                } catch (InterruptedException ignore) {
                }
            }
        };
        consumer.start();
        chunkReaderExecutor.scheduleAtFixedRate(chunkReader, 0, 250, TimeUnit.MILLISECONDS);
        consumer.join();

        // we can process one chunk at a time, so one read is queued, the second is buffered
        assertEquals("The e should be empty", 1, chunkQueue.remainingCapacity());

    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        fail("Thread " + t.getName() + " should not have thrown any uncaught exceptions! (" + e.getMessage() + ")");
    }

    public static class CountingAsyncFileChannel extends AsynchronousFileChannel {
        private final AsynchronousFileChannel other;
        private long readCount;

        public CountingAsyncFileChannel(AsynchronousFileChannel other) {
            this.other = other;
        }

        @Override
        public long size() throws IOException {
            return other.size();
        }

        @Override
        public AsynchronousFileChannel truncate(long size) throws IOException {
            return other.truncate(size);
        }

        @Override
        public void force(boolean metaData) throws IOException {
            other.force(metaData);
        }

        @Override
        public <A> void lock(long position, long size, boolean shared, A attachment, CompletionHandler<FileLock, ? super A> handler) {
            other.lock(position, size, shared, attachment, handler);
        }

        @Override
        public Future<FileLock> lock(long position, long size, boolean shared) {
            return other.lock(position, size, shared);
        }

        @Override
        public FileLock tryLock(long position, long size, boolean shared) throws IOException {
            return other.tryLock(position, size, shared);
        }

        @Override
        public <A> void read(ByteBuffer dst, long position, A attachment, CompletionHandler<Integer, ? super A> handler) {
            readCount++;
            other.read(dst, position, attachment, handler);
        }

        @Override
        public Future<Integer> read(ByteBuffer dst, long position) {
            readCount++;
            return other.read(dst, position);
        }

        @Override
        public <A> void write(ByteBuffer src, long position, A attachment, CompletionHandler<Integer, ? super A> handler) {
            other.write(src, position, attachment, handler);
        }

        @Override
        public Future<Integer> write(ByteBuffer src, long position) {
            return other.write(src, position);
        }

        @Override
        public void close() throws IOException {
            other.close();
        }

        @Override
        public boolean isOpen() {
            return other.isOpen();
        }

        public long getReadCount() {
            return readCount;
        }
    }

}
