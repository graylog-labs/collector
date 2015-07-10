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
import org.mockito.MockitoAnnotations;

import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SinglePathSetTest {
    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    private FileSystem newUnixFileSystem() {
        return Jimfs.newFileSystem(Configuration.unix());
    }

    private FileSystem newWindowsFileSystem() {
        return Jimfs.newFileSystem(Configuration.windows());
    }

    @Test
    public void testUnix() throws Exception {
        final FileSystem fileSystem = newUnixFileSystem();
        final Path path = fileSystem.getPath("/var/log/syslog");
        final PathSet pathSet = new SinglePathSet(path.toString(), fileSystem);

        assertEquals(path.getParent(), pathSet.getRootPath());
        assertTrue(pathSet.isInSet(path));
        assertTrue("Path list should be empty without any files in the file system",
                pathSet.getPaths().isEmpty());

        Files.createDirectories(path.getParent());
        Files.createFile(path);

        assertEquals("Path list should not be empty after creating the file",
                ImmutableSet.of(path), pathSet.getPaths());
    }

    @Test
    public void testWindows() throws Exception {
        final FileSystem fileSystem = newWindowsFileSystem();
        final Path path = fileSystem.getPath("C:\\logs\\application.log");
        final PathSet pathSet = new SinglePathSet(path.toString(), fileSystem);

        assertEquals(path.getParent(), pathSet.getRootPath());
        assertTrue(pathSet.isInSet(path));
        assertTrue("Path list should be empty without any files in the file system",
                pathSet.getPaths().isEmpty());

        Files.createDirectories(path.getParent());
        Files.createFile(path);

        assertEquals("Path list should not be empty after creating the file",
                ImmutableSet.of(path), pathSet.getPaths());
    }
}