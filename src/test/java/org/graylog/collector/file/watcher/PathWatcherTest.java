package org.graylog.collector.file.watcher;

import com.google.common.collect.Sets;
import org.joda.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

public class PathWatcherTest {
    private static final Logger log = LoggerFactory.getLogger(PathWatcherTest.class);

    private Duration duration = Duration.millis(500);
    private TemporaryFolder temporaryFolder1 = new TemporaryFolder();
    private TemporaryFolder temporaryFolder2 = new TemporaryFolder();
    private FileSystem fileSystem = FileSystems.getDefault();
    private Path rootPath1;
    private Path rootPath2;
    private PathWatcher watcher;

    @Before
    public void setUp() throws Exception {
        temporaryFolder1.create();
        temporaryFolder2.create();
        rootPath1 = temporaryFolder1.getRoot().toPath();
        rootPath2 = temporaryFolder2.getRoot().toPath();
        watcher = new PathWatcher(fileSystem.newWatchService(), duration);
    }

    @After
    public void tearDown() throws Exception {
        temporaryFolder1.delete();
        temporaryFolder2.delete();
    }

    private static class CountDownTestListener implements PathEventListener {
        private final String name;
        private final CountDownLatch created;
        private final CountDownLatch modified;
        private final CountDownLatch deleted;

        public CountDownTestListener(String name, CountDownLatch created, CountDownLatch modified, CountDownLatch deleted) {
            this.name = name;
            this.created = created;
            this.modified = modified;
            this.deleted = deleted;
        }

        @Override
        public void pathCreated(Path path) {
            log.info("({}) pathCreated {}", name, path);
            created.countDown();
        }

        @Override
        public void pathModified(Path path) {
            log.info("({}) pathModified {}", name, path);
            modified.countDown();
        }

        @Override
        public void pathRemoved(Path path) {
            log.info("({}) pathRemoved {}", name, path);
            deleted.countDown();
        }
    }

    @Test
    public void testCallbacks() throws Exception {
        Files.createFile(rootPath1.resolve("test.log"));
        Files.createFile(rootPath2.resolve("test2.log"));

        final CountDownLatch created1 = new CountDownLatch(2); // The watcher emits a CREATED event for existing files.
        final CountDownLatch modified1 = new CountDownLatch(1);
        final CountDownLatch deleted1 = new CountDownLatch(1);

        final CountDownLatch created2 = new CountDownLatch(2); // The watcher emits a CREATED event for existing files.
        final CountDownLatch modified2 = new CountDownLatch(1);
        final CountDownLatch deleted2 = new CountDownLatch(1);

        final CountDownLatch created3 = new CountDownLatch(2); // The watcher emits a CREATED event for existing files.
        final CountDownLatch modified3 = new CountDownLatch(1);
        final CountDownLatch deleted3 = new CountDownLatch(1);

        final PathEventListener listener1 = new CountDownTestListener("1", created1, modified1, deleted1);
        final PathEventListener listener2 = new CountDownTestListener("2", created2, modified2, deleted2);
        final PathEventListener listener3 = new CountDownTestListener("3", created3, modified3, deleted3);

        watcher.register(rootPath1, Sets.newHashSet(listener1, listener2));
        watcher.register(rootPath2, Sets.newHashSet(listener3));

        watcher.startAsync().awaitRunning(1, MINUTES);

        // File actions for the first root path.
        Files.createFile(rootPath1.resolve("test2.log"));
        // OS X is using a poll service here, the default poll frequency is 10s (we set it to 2, but that's platform specific)
        assertThat(created1.await(11, SECONDS)).isEqualTo(true);
        assertThat(created2.await(11, SECONDS)).isEqualTo(true);

        Files.write(rootPath1.resolve("test2.log"), "hello".getBytes());
        assertThat(modified1.await(11, SECONDS)).isEqualTo(true);
        assertThat(modified2.await(11, SECONDS)).isEqualTo(true);

        Files.delete(rootPath1.resolve("test2.log"));
        assertThat(deleted1.await(11, SECONDS)).isEqualTo(true);
        assertThat(deleted2.await(11, SECONDS)).isEqualTo(true);

        // File actions for the second root path.
        Files.createFile(rootPath2.resolve("test3.log"));
        assertThat(created3.await(11, SECONDS)).isEqualTo(true);

        Files.write(rootPath2.resolve("test3.log"), "world".getBytes());
        assertThat(modified3.await(11, SECONDS)).isEqualTo(true);

        Files.delete(rootPath2.resolve("test3.log"));
        assertThat(deleted3.await(11, SECONDS)).isEqualTo(true);

        watcher.stopAsync().awaitTerminated(1, MINUTES);
    }

    @Test
    public void testSubDirectories() throws Exception {
        final Path newDir = rootPath1.resolve("subdir");
        final Path newFile = newDir.resolve("subdir.log");

        final CountDownLatch subdirCreate = new CountDownLatch(1);
        final CountDownLatch subfileCreate = new CountDownLatch(1);
        final CountDownLatch subfileModify = new CountDownLatch(1);
        final CountDownLatch subfileDelete = new CountDownLatch(1);

        final PathEventListener listener = new PathEventListener() {
            @Override
            public void pathCreated(Path path) {
                log.info("pathCreated {}", path);
                if (newDir.equals(path)) {
                    subdirCreate.countDown();
                }
                if (newFile.equals(path)) {
                    subfileCreate.countDown();
                }
            }

            @Override
            public void pathModified(Path path) {
                log.info("pathModified {}", path);
                if (newFile.equals(path)) {
                    subfileModify.countDown();
                }
            }

            @Override
            public void pathRemoved(Path path) {
                log.info("pathRemoved {}", path);
                if (newFile.equals(path)) {
                    subfileDelete.countDown();
                }
            }
        };

        watcher.register(rootPath1, Sets.newHashSet(listener));
        watcher.startAsync().awaitRunning(1, MINUTES);

        Files.createDirectories(newDir);
        assertThat(subdirCreate.await(11, SECONDS)).isEqualTo(true);

        Files.createFile(newFile);
        assertThat(subfileCreate.await(11, SECONDS)).isEqualTo(true);

        Files.write(newFile, "hello".getBytes());
        assertThat(subfileModify.await(11, SECONDS)).isEqualTo(true);

        Files.delete(newFile);
        assertThat(subfileDelete.await(11, SECONDS)).isEqualTo(true);

        watcher.stopAsync().awaitTerminated(1, MINUTES);
    }
}