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
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.FileVisitor;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PathSetTest {
    @Mock
    BasicFileAttributes attributes;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testTrackingList() throws Exception {
        final PathSet list = new PathSet("/var/log/syslog", new PathSet.FileTreeWalker() {
            @Override
            public void walk(Path basePath, FileVisitor<Path> visitor) throws IOException {
                System.out.println("YO");
            }
        });

        final Set<Path> paths = list.getPaths();

        assertEquals(Paths.get("/var/log"), list.getRootPath());
        assertEquals(1, paths.size());
        assertTrue(paths.contains(Paths.get("/var/log/syslog")));
    }

    @Test
    public void testTrackingListWithGlob() throws Exception {
        final String file1 = "/var/log/upstart/graylog-collector.log";
        final String file2 = "/var/log/test/compressed.log.1.gz";
        final String file3 = "/var/log/foo/bar/baz/test.log";
        final String file4 = "/var/log/test.log";

        final PathSet list = new PathSet("/var/log/**/*.{log,gz}", new PathSet.FileTreeWalker() {
            @Override
            public void walk(Path basePath, FileVisitor<Path> visitor) throws IOException {
                visitor.visitFile(Paths.get(file1), attributes);
                visitor.visitFile(Paths.get(file2), attributes);
                visitor.visitFile(Paths.get(file3), attributes);
                visitor.visitFile(Paths.get(file4), attributes);
            }
        });

        final Set<Path> paths = list.getPaths();

        assertEquals(Paths.get("/var/log"), list.getRootPath());
        assertEquals(3, paths.size());
        assertTrue(paths.contains(Paths.get(file1)));
        assertTrue(paths.contains(Paths.get(file2)));
        assertTrue(paths.contains(Paths.get(file3)));
        assertFalse(paths.contains(Paths.get(file4)));
    }

    @Test
    public void testTrackingListWithGlobEdgeCases() throws Exception {
        final String file1 = "/var/log/ups?art/graylog-collector.log";

        final PathSet list = new PathSet("/var/log/ups\\?art/*.{log,gz}", new PathSet.FileTreeWalker() {
            @Override
            public void walk(Path basePath, FileVisitor<Path> visitor) throws IOException {
                visitor.visitFile(Paths.get(file1), attributes);
            }
        });

        final Set<Path> paths = list.getPaths();

        assertEquals(Paths.get("/var/log"), list.getRootPath());
        assertEquals(1, paths.size());
        assertTrue(paths.contains(Paths.get(file1)));
    }
}