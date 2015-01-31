/*
 * Copyright 2014 TORCH GmbH
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.graylog.agent.file.naming;

import org.testng.annotations.Test;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

public class FileNamingStrategyTest {

    @Test
    public void testNumberSuffixMatches() {
        final FileSystem fs = FileSystems.getDefault();
        final Path baseName = fs.getPath("/tmp", "logfile.log");
        final NumberSuffixStrategy m = new NumberSuffixStrategy(baseName);

        assertTrue(m.pathMatches(fs.getPath("/tmp", "logfile.log")), "same file matches");
        assertTrue(m.pathMatches(fs.getPath("/tmp", "logfile.log.1")), "number suffix matches");
        assertTrue(m.pathMatches(fs.getPath("/tmp", "logfile.log.1345345")), "multi-digit suffix matches");
        assertFalse(m.pathMatches(fs.getPath("/tmp", "logfile.log-123")), "separator must be '.'");
        assertFalse(m.pathMatches(fs.getPath("/tmp", "logfile.log.1234.gz")), "more suffixes don't match");
        assertFalse(m.pathMatches(fs.getPath("/var/log", "logfile.log.1234")), "wrong base path doesn't match");

        assertTrue(m.pathMatches(fs.getPath("/tmp/bar/..", "logfile.log.1")), "paths are normalized");
        assertTrue(m.pathMatches(fs.getPath("logfile.log.1")), "relative paths are resolved");

    }

    @Test
    public void testSameFile() {
        final FileSystem fs = FileSystems.getDefault();
        final Path baseName = fs.getPath("/tmp", "logfile.log");
        final ExactFileStrategy m = new ExactFileStrategy(baseName);

        assertTrue(m.pathMatches(fs.getPath("/tmp", "logfile.log")), "same file matches");
        assertTrue(m.pathMatches(fs.getPath("/tmp/foo/..", "logfile.log")), "paths are normalized");
        assertFalse(m.pathMatches(fs.getPath("/tmp", "logfile.log.1")), "only the same file name matches");
        assertTrue(m.pathMatches(fs.getPath("logfile.log")), "relative paths are resolved");

    }
}
