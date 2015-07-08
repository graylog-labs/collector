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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;

public class FileObserverTest {
    private static final Logger log = LoggerFactory.getLogger(FileObserverTest.class);

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    @Rule
    public TemporaryFolder temporaryFolder2 = new TemporaryFolder();

    private WatchService watchService;

    @Before
    public void setUp() throws Exception {
        watchService = FileSystems.getDefault().newWatchService();
    }

    @Test
    public void testObserverCallbacks() throws Exception {
        final PathSet pathSet = new PathSet(temporaryFolder.getRoot().toString() + "/*");
        final FileObserver fileObserver = new FileObserver(watchService);
        final File file = temporaryFolder.newFile();
        final Path path = file.toPath();
        // make sure the file doesn't exist prior to this test
        Files.deleteIfExists(path);

        final CountDownLatch createLatch = new CountDownLatch(1);
        final CountDownLatch deleteLatch = new CountDownLatch(1);
        final CountDownLatch modifiedLatch = new CountDownLatch(1);

        final FileObserver.Listener listener = new FileObserver.Listener() {
            @Override
            public void pathCreated(Path path) {
                log.info("Path created {}", path);
                createLatch.countDown();
            }

            @Override
            public void pathRemoved(Path path) {
                log.info("Path removed {}", path);
                deleteLatch.countDown();
            }

            @Override
            public void pathModified(Path path) {
                log.info("Path modified {}", path);
                modifiedLatch.countDown();
            }

            @Override
            public void cannotObservePath(Path path) {
                log.info("Cannot observe {}", path);
            }
        };

        fileObserver.observePathSet(pathSet, listener);

        fileObserver.startAsync();
        fileObserver.awaitRunning(1, TimeUnit.MINUTES);

        final boolean newFile = file.createNewFile();
        log.debug("Created new file {} with key {}", file.getPath(),
                Files.readAttributes(path, BasicFileAttributes.class).fileKey());
        assertTrue("Created monitored file", newFile);

        // OS X is using a poll service here, the default poll frequency is 10s (we set it to 2, but that's platform specific)
        final boolean awaitCreate = createLatch.await(10, TimeUnit.SECONDS);
        assertTrue("Monitored creation change event must be delivered.", awaitCreate);

        Files.write(path, "hello".getBytes());
        final boolean awaitModify = modifiedLatch.await(10, TimeUnit.SECONDS);
        assertTrue("Monitored modification change event must be delivered.", awaitModify);

        assertTrue("Must be able to remove log file", file.delete());
        final boolean awaitRemove = deleteLatch.await(10, TimeUnit.SECONDS);
        assertTrue("Monitored removal change event must be delivered.", awaitRemove);

        fileObserver.stopAsync();
        fileObserver.awaitTerminated(1, TimeUnit.MINUTES);
    }

    @Test
    public void testObserverCallbacksForMultipleFiles() throws Exception {
        final FileObserver fileObserver = new FileObserver(watchService);
        final File file1 = temporaryFolder.newFile();
        final File file2 = temporaryFolder.newFile();
        final File file3 = temporaryFolder2.newFile();
        final PathSet pathSet1 = new PathSet(temporaryFolder.getRoot().toString() + "/*");
        final PathSet pathSet2 = new PathSet(temporaryFolder2.getRoot().toString() + "/*");
        final Path path1 = file1.toPath();
        final Path path2 = file2.toPath();
        final Path path3 = file3.toPath();
        // make sure the file doesn't exist prior to this test
        Files.deleteIfExists(path1);
        Files.deleteIfExists(path2);
        Files.deleteIfExists(path3);

        final CountDownLatch createLatch1 = new CountDownLatch(1);
        final CountDownLatch createLatch2 = new CountDownLatch(1);
        final CountDownLatch createLatch3 = new CountDownLatch(1);
        final CountDownLatch deleteLatch1 = new CountDownLatch(1);
        final CountDownLatch deleteLatch2 = new CountDownLatch(1);
        final CountDownLatch deleteLatch3 = new CountDownLatch(1);
        final CountDownLatch modifiedLatch1 = new CountDownLatch(1);
        final CountDownLatch modifiedLatch2 = new CountDownLatch(1);
        final CountDownLatch modifiedLatch3 = new CountDownLatch(1);

        final FileObserver.Listener listener = new FileObserver.Listener() {
            @Override
            public void pathCreated(Path path) {
                log.info("Path created {}", path);
                if (path.equals(path1)) {
                    createLatch1.countDown();
                } else if (path.equals(path2)) {
                    createLatch2.countDown();
                } else if (path.equals(path3)) {
                    createLatch3.countDown();
                }
            }

            @Override
            public void pathRemoved(Path path) {
                log.info("Path removed {}", path);
                if (path.equals(path1)) {
                    deleteLatch1.countDown();
                } else if (path.equals(path2)) {
                    deleteLatch2.countDown();
                } else if (path.equals(path3)) {
                    deleteLatch3.countDown();
                }
            }

            @Override
            public void pathModified(Path path) {
                log.info("Path modified {}", path);
                if (path.equals(path1)) {
                    modifiedLatch1.countDown();
                } else if (path.equals(path2)) {
                    modifiedLatch2.countDown();
                } else if (path.equals(path3)) {
                    modifiedLatch3.countDown();
                }
            }

            @Override
            public void cannotObservePath(Path path) {
                log.info("Cannot observe {}", path);
            }
        };

        fileObserver.observePathSet(pathSet1, listener);
        fileObserver.observePathSet(pathSet2, listener);

        fileObserver.startAsync();
        fileObserver.awaitRunning(1, TimeUnit.MINUTES);

        final boolean newFile1 = file1.createNewFile();
        final boolean newFile2 = file2.createNewFile();
        final boolean newFile3 = file3.createNewFile();
        log.debug("Created new file {} with key {}", file1.getPath(), Files.readAttributes(path1, BasicFileAttributes.class).fileKey());
        log.debug("Created new file {} with key {}", file2.getPath(), Files.readAttributes(path2, BasicFileAttributes.class).fileKey());
        log.debug("Created new file {} with key {}", file3.getPath(), Files.readAttributes(path3, BasicFileAttributes.class).fileKey());
        assertTrue("Created monitored file", newFile1);
        assertTrue("Created monitored file", newFile2);
        assertTrue("Created monitored file", newFile3);

        // OS X is using a poll service here, the default poll frequency is 10s (we set it to 2, but that's platform specific)
        final boolean awaitCreate1 = createLatch1.await(10, TimeUnit.SECONDS);
        final boolean awaitCreate2 = createLatch2.await(10, TimeUnit.SECONDS);
        final boolean awaitCreate3 = createLatch3.await(10, TimeUnit.SECONDS);
        assertTrue("Monitored creation change event must be delivered.", awaitCreate1);
        assertTrue("Monitored creation change event must be delivered.", awaitCreate2);
        assertTrue("Monitored creation change event must be delivered.", awaitCreate3);

        Files.write(path1, "hello1".getBytes());
        Files.write(path2, "hello2".getBytes());
        Files.write(path3, "hello3".getBytes());
        final boolean awaitModify1 = modifiedLatch1.await(10, TimeUnit.SECONDS);
        final boolean awaitModify2 = modifiedLatch2.await(10, TimeUnit.SECONDS);
        final boolean awaitModify3 = modifiedLatch3.await(10, TimeUnit.SECONDS);
        assertTrue("Monitored modification change event must be delivered.", awaitModify1);
        assertTrue("Monitored modification change event must be delivered.", awaitModify2);
        assertTrue("Monitored modification change event must be delivered.", awaitModify3);

        assertTrue("Must be able to remove log file", file1.delete());
        assertTrue("Must be able to remove log file", file2.delete());
        assertTrue("Must be able to remove log file", file3.delete());
        final boolean awaitRemove1 = deleteLatch1.await(10, TimeUnit.SECONDS);
        final boolean awaitRemove2 = deleteLatch2.await(10, TimeUnit.SECONDS);
        final boolean awaitRemove3 = deleteLatch3.await(10, TimeUnit.SECONDS);
        assertTrue("Monitored removal change event must be delivered.", awaitRemove1);
        assertTrue("Monitored removal change event must be delivered.", awaitRemove2);
        assertTrue("Monitored removal change event must be delivered.", awaitRemove3);

        fileObserver.stopAsync();
        fileObserver.awaitTerminated(1, TimeUnit.MINUTES);
    }

    @Test
    @Ignore("File naming strategies have been disabled for now")
    public void testNamingStrategy() throws Exception {
        final PathSet pathSet = new PathSet(temporaryFolder.getRoot().toString() + "/*");
        final FileObserver fileObserver = new FileObserver(watchService);
        final File file1 = temporaryFolder.newFile();
        final File file2 = temporaryFolder.newFile();
        final Path path1 = file1.toPath();
        final Path path2 = file2.toPath();
        // make sure the file doesn't exist prior to this test
        Files.deleteIfExists(path1);
        Files.deleteIfExists(path2);

        final CountDownLatch createLatch = new CountDownLatch(1);
        final CountDownLatch deleteLatch = new CountDownLatch(1);
        final CountDownLatch modifyLatch = new CountDownLatch(1);

        final AtomicInteger createCountPath1 = new AtomicInteger(0);
        final AtomicInteger deleteCountPath1 = new AtomicInteger(0);
        final AtomicInteger modifyCountPath1 = new AtomicInteger(0);

        final FileObserver.Listener listener = new FileObserver.Listener() {
            @Override
            public void pathCreated(Path path) {
                log.info("Path created {}", path);
                if (path.equals(path1)) {
                    createCountPath1.incrementAndGet();
                } else if (path.equals(path2)) {
                    createLatch.countDown();
                }
            }

            @Override
            public void pathRemoved(Path path) {
                log.info("Path removed {}", path);
                if (path.equals(path1)) {
                    deleteCountPath1.incrementAndGet();
                } else if (path.equals(path2)) {
                    deleteLatch.countDown();
                }
            }

            @Override
            public void pathModified(Path path) {
                log.info("Path modified {}", path);
                if (path.equals(path1)) {
                    modifyCountPath1.incrementAndGet();
                } else if (path.equals(path2)) {
                    modifyLatch.countDown();
                }
            }

            @Override
            public void cannotObservePath(Path path) {
                log.info("Cannot observe {}", path);
            }
        };

        // Make sure to use a non-existent path for the naming strategy here!
        /*
        fileObserver.observePath(listener, path1, new ExactFileStrategy(Paths.get("/tmp/foo/bar/baz")));
        fileObserver.observePath(listener, path2, new ExactFileStrategy(path2));
        */
        fileObserver.observePathSet(pathSet, listener);

        fileObserver.startAsync();
        fileObserver.awaitRunning(1, TimeUnit.MINUTES);

        final boolean newFile1 = file1.createNewFile();
        final boolean newFile2 = file2.createNewFile();
        log.debug("Created new file {} with key {}", file1.getPath(), Files.readAttributes(path1, BasicFileAttributes.class).fileKey());
        log.debug("Created new file {} with key {}", file2.getPath(), Files.readAttributes(path2, BasicFileAttributes.class).fileKey());
        assertTrue("Created monitored file", newFile1);
        assertTrue("Created monitored file", newFile2);

        // OS X is using a poll service here, the default poll frequency is 10s (we set it to 2, but that's platform specific)
        final boolean awaitCreate1 = createLatch.await(10, TimeUnit.SECONDS);
        assertTrue("Monitored creation change event must be delivered.", awaitCreate1);
        assertTrue("Monitored creation change event must NOT be delivered.", createCountPath1.get() == 0);

        Files.write(path1, "hello1".getBytes());
        Files.write(path2, "hello2".getBytes());
        final boolean awaitModify1 = modifyLatch.await(10, TimeUnit.SECONDS);
        assertTrue("Monitored modification change event must be delivered.", awaitModify1);
        assertTrue("Monitored modification change event must NOT be delivered.", modifyCountPath1.get() == 0);

        assertTrue("Must be able to remove log file", file1.delete());
        assertTrue("Must be able to remove log file", file2.delete());
        final boolean awaitRemove1 = deleteLatch.await(10, TimeUnit.SECONDS);
        assertTrue("Monitored removal change event must be delivered.", awaitRemove1);
        assertTrue("Monitored removal change event must NOT be delivered.", deleteCountPath1.get() == 0);

        fileObserver.stopAsync();
        fileObserver.awaitTerminated(1, TimeUnit.MINUTES);
    }
}