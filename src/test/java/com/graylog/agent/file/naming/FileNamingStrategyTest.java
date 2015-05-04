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
package com.graylog.agent.file.naming;

import org.junit.Test;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FileNamingStrategyTest {

    @Test
    public void testNumberSuffixMatches() {
        final FileSystem fs = FileSystems.getDefault();
        final Path baseName = fs.getPath("/tmp", "logfile.log");
        final NumberSuffixStrategy m = new NumberSuffixStrategy(baseName);

        assertTrue("same file matches", m.pathMatches(fs.getPath("/tmp", "logfile.log")));
        assertTrue("number suffix matches", m.pathMatches(fs.getPath("/tmp", "logfile.log.1")));
        assertTrue("multi-digit suffix matches", m.pathMatches(fs.getPath("/tmp", "logfile.log.1345345")));
        assertFalse("separator must be '.'", m.pathMatches(fs.getPath("/tmp", "logfile.log-123")));
        assertFalse("more suffixes don't match", m.pathMatches(fs.getPath("/tmp", "logfile.log.1234.gz")));
        assertFalse("wrong base path doesn't match", m.pathMatches(fs.getPath("/var/log", "logfile.log.1234")));

        assertTrue("paths are normalized", m.pathMatches(fs.getPath("/tmp/bar/..", "logfile.log.1")));
        assertTrue("relative paths are resolved", m.pathMatches(fs.getPath("logfile.log.1")));

    }

    @Test
    public void testSameFile() {
        final FileSystem fs = FileSystems.getDefault();
        final Path baseName = fs.getPath("/tmp", "logfile.log");
        final ExactFileStrategy m = new ExactFileStrategy(baseName);

        assertTrue("same file matches", m.pathMatches(fs.getPath("/tmp", "logfile.log")));
        assertTrue("paths are normalized", m.pathMatches(fs.getPath("/tmp/foo/..", "logfile.log")));
        assertFalse("only the same file name matches", m.pathMatches(fs.getPath("/tmp", "logfile.log.1")));
        assertTrue("relative paths are resolved", m.pathMatches(fs.getPath("logfile.log")));
    }
}
