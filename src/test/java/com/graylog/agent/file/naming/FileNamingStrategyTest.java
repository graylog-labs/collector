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
