package com.graylog.agent.config;

import com.google.common.collect.Sets;
import com.typesafe.config.Config;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;

import static org.junit.Assert.assertEquals;

public class ConfigurationParserTest {
    private File configFile;

    @Rule
    public ExpectedException thrower = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        configFile = Files.createTempFile("agent", ".conf").toFile();

        Files.write(configFile.toPath(), "message-buffer-size = 128".getBytes());
    }

    @Test
    public void testParsing() throws Exception{
        final Config config = ConfigurationParser.parse(configFile);

        assertEquals(128, config.getInt("message-buffer-size"));
    }

    @Test
    public void testMissingConfigFile() throws Exception {
        thrower.expect(ConfigurationParser.Error.class);

        configFile.delete();

        ConfigurationParser.parse(configFile);
    }

    @Test
    public void testUnreadableFile() throws Exception {
        thrower.expect(ConfigurationParser.Error.class);

        Files.setPosixFilePermissions(configFile.toPath(), Sets.newHashSet(new PosixFilePermission[]{}));

        ConfigurationParser.parse(configFile);
    }

    @Test
    public void testEmptyFile() throws Exception {
        thrower.expect(ConfigurationParser.Error.class);

        configFile.delete();
        Files.write(configFile.toPath(), "".getBytes());

        ConfigurationParser.parse(configFile);
    }
}