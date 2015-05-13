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
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Service;
import org.graylog.collector.Message;
import org.graylog.collector.MessageBuilder;
import org.graylog.collector.file.naming.NumberSuffixStrategy;
import org.graylog.collector.file.splitters.NewlineChunkSplitter;
import org.graylog.collector.inputs.file.FileInput;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileReaderServiceTest extends MultithreadedBaseTest {
    private static final Logger log = LoggerFactory.getLogger(FileReaderServiceTest.class);

    private static final FileSystem fs = FileSystems.getDefault();

    @Test
    public void testObserverCallbacks() throws IOException, InterruptedException {
        Path path = fs.getPath("/tmp", "logfile.log");
        File file = path.toFile();
        // make sure the file doesn't exist prior to this test
        Files.deleteIfExists(path);

        final CountDownLatch createLatch = new CountDownLatch(1);
        final CountDownLatch deleteLatch = new CountDownLatch(1);

        final FileInput mockInput = mockFileInput();

        final CollectingBuffer buffer = new CollectingBuffer();
        final FileReaderService readerService = new FileReaderService(
                path,
                Charsets.UTF_8,
                new NumberSuffixStrategy(path),
                true,
                FileInput.InitialReadPosition.START,
                mockInput,
                null,
                new NewlineChunkSplitter(),
                buffer);

        final FileObserver.Listener origChangeListener = readerService.getChangeListener();
        final FileObserver.Listener listener = new FileObserver.Listener() {
            @Override
            public void pathCreated(Path path) {
                log.info("Path created {}", path);
                origChangeListener.pathCreated(path);
                createLatch.countDown();
            }

            @Override
            public void pathRemoved(Path path) {
                log.info("Path removed {}", path);
                origChangeListener.pathRemoved(path);
                deleteLatch.countDown();
            }

            @Override
            public void pathModified(Path path) {
                log.info("Path modified {}", path);
                origChangeListener.pathModified(path);
            }

            @Override
            public void cannotObservePath(Path path) {
                log.info("Cannot observe {}", path);
                origChangeListener.cannotObservePath(path);
            }
        };
        readerService.setChangeListener(listener);

        readerService.startAsync();
        readerService.awaitRunning();

        assertEquals("service should be running", Service.State.RUNNING, readerService.state());

        final boolean newFile = file.createNewFile();
        log.debug("Created new file {} with key {}", file.getPath(),
                Files.readAttributes(path, BasicFileAttributes.class).fileKey());
        assertTrue("Created monitored file", newFile);

        // OS X is using a poll service here, the default poll frequency is 10s (we set it to 2, but that's platform specific)
        final boolean awaitCreate = createLatch.await(10, TimeUnit.SECONDS);
        assertTrue("Monitored creation change event must be delivered.", awaitCreate);

        assertTrue("Must be able to remove log file", file.delete());
        final boolean awaitRemove = deleteLatch.await(10, TimeUnit.SECONDS);
        assertTrue("Monitored removal change event must be delivered.", awaitRemove);

        readerService.stopAsync();
        readerService.awaitTerminated();
    }

    private FileInput mockFileInput() {
        final FileInput mockInput = mock(FileInput.class);
        when(mockInput.getId()).thenReturn("testinputid");
        return mockInput;
    }

    @Test
    public void fileCreatedAfterStartIsRead() throws IOException, InterruptedException {
        Path path = fs.getPath("/tmp", "logfile.log");
        File file = path.toFile();
        // make sure the file doesn't exist prior to this test
        Files.deleteIfExists(path);

        final FileInput mockInput = mockFileInput();

        final CollectingBuffer buffer = new CollectingBuffer();
        final MessageBuilder messageBuilder = new MessageBuilder().input("input-id").outputs(new HashSet<String>()).source("test");
        final FileReaderService readerService = new FileReaderService(
                path,
                Charsets.UTF_8,
                new NumberSuffixStrategy(path),
                true,
                FileInput.InitialReadPosition.START,
                mockInput,
                messageBuilder,
                new NewlineChunkSplitter(),
                buffer);

        readerService.startAsync();
        readerService.awaitRunning();

        // create new file and write a line to it
        Files.write(file.toPath(), "hellotest\n".getBytes(), StandardOpenOption.CREATE_NEW, StandardOpenOption.APPEND, StandardOpenOption.WRITE);

        // the reader service should detect the file creation event, create a chunkreader for it and eventually a message
        // should appear in the buffer
        // let's wait for that
        final Message msg = buffer.getMessageQueue().poll(20, TimeUnit.SECONDS);
        assertNotNull("file reader should have created a message", msg);
        assertEquals("message content matches", "hellotest", msg.getMessage());
        assertEquals("no more messages have been added to the buffer", 0, buffer.getMessageQueue().size());

        readerService.stopAsync();
        readerService.awaitTerminated();
    }

    @Test
    public void fileCreatedAfterStartIsReadAfterPermissionsFixed() throws IOException, InterruptedException {
        Path path = fs.getPath("/tmp", "logfile.log");
        File file = path.toFile();
        // make sure the file doesn't exist prior to this test
        Files.deleteIfExists(path);

        final FileInput mockInput = mockFileInput();

        final CollectingBuffer buffer = new CollectingBuffer();
        final MessageBuilder messageBuilder = new MessageBuilder().input("input-id").outputs(new HashSet<String>()).source("test");
        final FileReaderService readerService = new FileReaderService(
                path,
                Charsets.UTF_8,
                new NumberSuffixStrategy(path),
                true,
                FileInput.InitialReadPosition.START,
                mockInput,
                messageBuilder,
                new NewlineChunkSplitter(),
                buffer);

        readerService.startAsync();
        readerService.awaitRunning();

        // create new unreadable file and write a line to it
        final SeekableByteChannel channel = Files.newByteChannel(file.toPath(),
                Sets.newHashSet(StandardOpenOption.CREATE_NEW, StandardOpenOption.APPEND),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("---------")));
        channel.write(ByteBuffer.wrap("hellotest\n".getBytes()));

        // Check that there has been no message read from the file.
        final Message msgNull = buffer.getMessageQueue().poll(500, TimeUnit.MILLISECONDS);
        assertNull("There should be no message read from the file!", msgNull);

        // Make file readable.
        Files.setPosixFilePermissions(file.toPath(), Sets.newHashSet(PosixFilePermission.OWNER_READ));

        // the reader service should detect the file modifcation event, create a chunkreader for it and eventually
        // a message should appear in the buffer
        // let's wait for that
        final Message msg = buffer.getMessageQueue().poll(10, TimeUnit.SECONDS);
        assertNotNull("file reader should have created a message", msg);
        assertEquals("message content matches", "hellotest", msg.getMessage());
        assertEquals("no more messages have been added to the buffer", 0, buffer.getMessageQueue().size());

        readerService.stopAsync();
        readerService.awaitTerminated();
    }
}
