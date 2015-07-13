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

import com.google.common.collect.ImmutableSet;
import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class GlobPathSetTest {
    public static final GlobPathSet.FileTreeWalker NOOP_FILE_TREE_WALKER = new GlobPathSet.FileTreeWalker() {
        @Override
        public void walk(Path basePath, FileVisitor<Path> visitor) throws IOException {
        }
    };

    @Mock
    BasicFileAttributes attributes;

    private FileSystem fileSystem;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        this.fileSystem = newUnixFileSystem();
    }

    private FileSystem newUnixFileSystem() {
        return Jimfs.newFileSystem(Configuration.unix());
    }

    private FileSystem newWindowsFileSystem() {
        return Jimfs.newFileSystem(Configuration.windows());
    }

    @Test
    public void testTrackingList() throws Exception {
        final Path path = fileSystem.getPath("/var/log/syslog");

        Files.createDirectories(path.getParent());
        Files.createFile(path);

        final PathSet pathSet = new GlobPathSet("/var/log", "syslog", new GlobPathSet.FileTreeWalker() {
            @Override
            public void walk(Path basePath, FileVisitor<Path> visitor) throws IOException {
                visitor.visitFile(path, attributes);
            }
        }, fileSystem);
        final Set<Path> paths = pathSet.getPaths();

        assertEquals(path.getParent(), pathSet.getRootPath());
        assertEquals(1, paths.size());
        assertTrue(paths.contains(fileSystem.getPath("/var/log/syslog")));
    }

    @Test
    public void testTrackingListWithGlob() throws Exception {
        final String file1 = "/var/log/upstart/graylog-collector.log";
        final String file2 = "/var/log/test/compressed.log.1.gz";
        final String file3 = "/var/log/foo/bar/baz/test.log";
        final String file4 = "/var/log/test.log";

        Files.createDirectories(fileSystem.getPath(file1).getParent());
        Files.createDirectories(fileSystem.getPath(file2).getParent());
        Files.createDirectories(fileSystem.getPath(file3).getParent());
        Files.createDirectories(fileSystem.getPath(file4).getParent());

        final PathSet list = new GlobPathSet("/var/log", "**/*.{log,gz}", new GlobPathSet.FileTreeWalker() {
            @Override
            public void walk(Path basePath, FileVisitor<Path> visitor) throws IOException {
                visitor.visitFile(fileSystem.getPath(file1), attributes);
                visitor.visitFile(fileSystem.getPath(file2), attributes);
                visitor.visitFile(fileSystem.getPath(file3), attributes);
                visitor.visitFile(fileSystem.getPath(file4), attributes);
            }
        }, fileSystem);

        final Set<Path> paths = list.getPaths();

        assertEquals(fileSystem.getPath("/var/log"), list.getRootPath());
        assertEquals(3, paths.size());
        assertTrue(paths.contains(fileSystem.getPath(file1)));
        assertTrue(paths.contains(fileSystem.getPath(file2)));
        assertTrue(paths.contains(fileSystem.getPath(file3)));
        assertFalse(paths.contains(fileSystem.getPath(file4)));
    }

    @Test
    public void testTrackingListWithGlobEdgeCases() throws Exception {
        final String file1 = "/var/log/ups?art/graylog-collector.log";
        final Path path = fileSystem.getPath(file1);

        Files.createDirectories(path.getParent());

        final PathSet pathSet = new GlobPathSet("/var/log/ups?art", "*.{log,gz}", new GlobPathSet.FileTreeWalker() {
            @Override
            public void walk(Path basePath, FileVisitor<Path> visitor) throws IOException {
                visitor.visitFile(path, attributes);
            }
        }, fileSystem);

        final Set<Path> paths = pathSet.getPaths();

        assertEquals(path.getParent(), pathSet.getRootPath());
        assertEquals(1, paths.size());
        assertTrue(paths.contains(path));
    }

    @Test
    public void testIsInSet() throws Exception {
        final PathSet pathSet = new GlobPathSet("/var/log", "**/*.{log,gz}", NOOP_FILE_TREE_WALKER, fileSystem);

        assertFalse(pathSet.isInSet(fileSystem.getPath("/var/log/mail.log")));
        assertTrue(pathSet.isInSet(fileSystem.getPath("/var/log/upstart/test.log")));
        assertTrue(pathSet.isInSet(fileSystem.getPath("/var/log/upstart/ntp.log.gz")));
        assertFalse(pathSet.isInSet(fileSystem.getPath("/var/log/ntp/compressed.txt")));
    }

    @Test
    public void testIsInSetWithoutPattern() throws Exception {
        final PathSet pathSet = new GlobPathSet("/var/log", "syslog", NOOP_FILE_TREE_WALKER, fileSystem);

        assertFalse(pathSet.isInSet(fileSystem.getPath("/var/log/mail.log")));
        assertTrue(pathSet.isInSet(fileSystem.getPath("/var/log/syslog")));
    }

    @Test
    public void testEquality() throws Exception{
        final GlobPathSet.FileTreeWalker treeWalker = NOOP_FILE_TREE_WALKER;

        final PathSet pathSet1 = new GlobPathSet("/var/log", "syslog", treeWalker, fileSystem);
        final PathSet pathSet2 = new GlobPathSet("/var/log", "syslog", treeWalker, fileSystem);
        final PathSet pathSet3 = new GlobPathSet("/var/log", "**/*.log", treeWalker, fileSystem);
        final PathSet pathSet4 = new GlobPathSet("/var/log", "**/*.log", treeWalker, fileSystem);

        assertEquals(pathSet1, pathSet2);
        assertEquals(pathSet3, pathSet4);
        assertNotEquals(pathSet1, pathSet4);
    }

    @Test
    public void testWindows() throws Exception {
        final FileSystem winFs = newWindowsFileSystem();

        Files.createDirectories(winFs.getPath("C:\\test"));

        final PathSet pathSet = new GlobPathSet("C:\\test", "*.log", new GlobPathSet.FileTreeWalker() {
            @Override
            public void walk(Path basePath, FileVisitor<Path> visitor) throws IOException {
                Files.walkFileTree(basePath, visitor);
            }
        }, winFs);

        assertEquals(winFs.getPath("C:\\test"), pathSet.getRootPath());
        assertTrue(pathSet.getPaths().isEmpty());

        Files.createFile(winFs.getPath("C:\\test\\test.log"));

        assertEquals(ImmutableSet.of(winFs.getPath("C:\\test\\test.log")), pathSet.getPaths());
    }
}