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
import org.graylog.collector.file.naming.ExactFileStrategy;
import org.graylog.collector.file.naming.FileNamingStrategy;
import org.graylog.collector.file.naming.GlobbingStrategy;
import org.graylog.collector.file.naming.NumberSuffixStrategy;
import org.graylog.collector.file.splitters.NewlineChunkSplitter;
import org.graylog.collector.file.watcher.PathEventListener;
import org.graylog.collector.file.watcher.PathWatcher;
import org.graylog.collector.inputs.file.FileInput;
import org.joda.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FileReaderServiceTest extends MultithreadedBaseTest {
    private static final Logger log = LoggerFactory.getLogger(FileReaderServiceTest.class);

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private PathWatcher pathWatcher;

    @Before
    public void setUp() throws Exception {
        temporaryFolder.create();

        pathWatcher = new PathWatcher(FileSystems.getDefault().newWatchService(), Duration.millis(500));
        pathWatcher.startAsync().awaitRunning(1, TimeUnit.MINUTES);
    }

    @After
    public void shutDown() throws Exception {
        pathWatcher.stopAsync().awaitTerminated(1, TimeUnit.MINUTES);

        temporaryFolder.delete();
    }

    @Test
    public void testObserverCallbacks() throws Exception {
        final Path path = temporaryFolder.newFile().toPath();

        final PathWatcher fileObserverSpy = spy(pathWatcher);
        final NumberSuffixStrategy namingStrategy = new NumberSuffixStrategy(path);

        final FileReaderService readerService = new FileReaderService(
                path,
                namingStrategy,
                Charsets.UTF_8,
                FileInput.InitialReadPosition.START,
                mockFileInput(),
                null,
                new NewlineChunkSplitter(),
                new CollectingBuffer(),
                1024,
                250L,
                fileObserverSpy);

        readerService.startAsync();
        readerService.awaitRunning(1, TimeUnit.MINUTES);

        assertEquals("service should be running", Service.State.RUNNING, readerService.state());

        verify(fileObserverSpy).register(eq(path), anySetOf(PathEventListener.class));

        readerService.stopAsync();
        readerService.awaitTerminated(1, TimeUnit.MINUTES);
    }

    private FileInput mockFileInput() {
        final FileInput mockInput = mock(FileInput.class);
        when(mockInput.getId()).thenReturn("testinputid");
        return mockInput;
    }

    @Test
    public void fileCreatedAfterStartIsRead() throws Exception {
        final Path rootPath = temporaryFolder.getRoot().toPath();
        final Path file = temporaryFolder.newFile().toPath();
        // make sure the file doesn't exist prior to this test
        Files.deleteIfExists(file);

        final FileInput mockInput = mockFileInput();

        final CollectingBuffer buffer = new CollectingBuffer();
        final MessageBuilder messageBuilder = new MessageBuilder().input("input-id").outputs(new HashSet<String>()).source("test");
        final FileReaderService readerService = new FileReaderService(
                rootPath,
                new ExactFileStrategy(file),
                Charsets.UTF_8,
                FileInput.InitialReadPosition.START,
                mockInput,
                messageBuilder,
                new NewlineChunkSplitter(),
                buffer,
                1024,
                250L,
                pathWatcher);

        readerService.startAsync();
        readerService.awaitRunning();

        // create new file and write a line to it
        Files.write(file, "hellotest\n".getBytes(), StandardOpenOption.CREATE_NEW, StandardOpenOption.APPEND, StandardOpenOption.WRITE);

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
    public void fileCreatedAfterStartIsReadAfterPermissionsFixed() throws Exception {
        final Path rootPath = temporaryFolder.getRoot().toPath();
        final Path file = temporaryFolder.newFile().toPath();
        // make sure the file doesn't exist prior to this test
        Files.deleteIfExists(file);

        final FileInput mockInput = mockFileInput();

        final CollectingBuffer buffer = new CollectingBuffer();
        final MessageBuilder messageBuilder = new MessageBuilder().input("input-id").outputs(new HashSet<String>()).source("test");
        final FileReaderService readerService = new FileReaderService(
                rootPath,
                new ExactFileStrategy(file),
                Charsets.UTF_8,
                FileInput.InitialReadPosition.START,
                mockInput,
                messageBuilder,
                new NewlineChunkSplitter(),
                buffer,
                1024,
                250L,
                pathWatcher);

        readerService.startAsync();
        readerService.awaitRunning();

        // create new unreadable file and write a line to it
        final SeekableByteChannel channel = Files.newByteChannel(file,
                Sets.newHashSet(StandardOpenOption.CREATE_NEW, StandardOpenOption.APPEND),
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("---------")));
        channel.write(ByteBuffer.wrap("hellotest\n".getBytes()));

        // Check that there has been no message read from the file.
        final Message msgNull = buffer.getMessageQueue().poll(500, TimeUnit.MILLISECONDS);
        assertNull("There should be no message read from the file!", msgNull);

        // Make file readable.
        Files.setPosixFilePermissions(file, Sets.newHashSet(PosixFilePermission.OWNER_READ));

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

    @Test
    public void followMultipleFiles() throws Exception {
        final Path rootPath = temporaryFolder.getRoot().toPath();
        final Path file1 = temporaryFolder.newFile().toPath();
        final Path file2 = temporaryFolder.newFile().toPath();
        final Path file3 = temporaryFolder.newFile().toPath();

        log.info("FILE 1 - {}", file1);
        log.info("FILE 2 - {}", file2);
        log.info("FILE 3 - {}", file3);

        final FileNamingStrategy namingStrategy= new GlobbingStrategy(rootPath, "*");

        // Delete the second file before starting.
        Files.deleteIfExists(file2);

        final CollectingBuffer buffer = new CollectingBuffer();
        final MessageBuilder messageBuilder = new MessageBuilder().input("input-id").outputs(new HashSet<String>()).source("test");
        final FileReaderService readerService = new FileReaderService(
                rootPath,
                namingStrategy,
                Charsets.UTF_8,
                FileInput.InitialReadPosition.START,
                mockFileInput(),
                messageBuilder,
                new NewlineChunkSplitter(),
                buffer,
                1024,
                250L,
                pathWatcher);

        readerService.startAsync();
        readerService.awaitRunning();

        Files.write(file1, "file1\n".getBytes());

        final Message msg1 = buffer.getMessageQueue().poll(20, TimeUnit.SECONDS);
        assertNotNull("file reader should have created a message", msg1);
        assertEquals("message content matches", "file1", msg1.getMessage());
        assertEquals("no more messages have been added to the buffer", 0, buffer.getMessageQueue().size());

        Files.write(file2, "file2\n".getBytes(), StandardOpenOption.CREATE_NEW, StandardOpenOption.APPEND, StandardOpenOption.WRITE);

        final Message msg2 = buffer.getMessageQueue().poll(20, TimeUnit.SECONDS);
        assertNotNull("file reader should have created a message", msg2);
        assertEquals("message content matches", "file2", msg2.getMessage());
        assertEquals("no more messages have been added to the buffer", 0, buffer.getMessageQueue().size());

        Files.write(file3, "file3\n".getBytes());

        final Message msg3 = buffer.getMessageQueue().poll(20, TimeUnit.SECONDS);
        assertNotNull("file reader should have created a message", msg3);
        assertEquals("message content matches", "file3", msg3.getMessage());
        assertEquals("no more messages have been added to the buffer", 0, buffer.getMessageQueue().size());

        readerService.stopAsync();
        readerService.awaitTerminated();
    }
}
