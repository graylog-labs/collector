package com.graylog.agent.file;

import com.google.common.util.concurrent.Service;
import com.graylog.agent.Message;
import com.graylog.agent.MessageBuilder;
import com.graylog.agent.file.naming.NumberSuffixStrategy;
import com.graylog.agent.file.splitters.NewlineChunkSplitter;
import com.graylog.agent.inputs.file.FileInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

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

        assertEquals(readerService.state(), Service.State.RUNNING, "service should be running");

        final boolean newFile = file.createNewFile();
        log.debug("Created new file {} with key {}", file.getPath(),
                  Files.readAttributes(path, BasicFileAttributes.class).fileKey());
        assertTrue(newFile, "Created monitored file");

        // OS X is using a poll service here, the default poll frequency is 10s (we set it to 2, but that's platform specific)
        final boolean awaitCreate = createLatch.await(10, TimeUnit.SECONDS);
        assertTrue(awaitCreate, "Monitored creation change event must be delivered.");

        assertTrue(file.delete(), "Must be able to remove log file");
        final boolean awaitRemove = deleteLatch.await(10, TimeUnit.SECONDS);
        assertTrue(awaitRemove, "Monitored removal change event must be delivered.");

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
        assertNotNull(msg, "file reader should have created a message");
        assertEquals(msg.getMessage(), "hellotest", "message content matches");
        assertEquals(buffer.getMessageQueue().size(), 0, "no more messages have been added to the buffer");

        readerService.stopAsync();
        readerService.awaitTerminated();
    }

}
