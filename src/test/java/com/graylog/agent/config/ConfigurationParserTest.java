package com.graylog.agent.config;

import com.google.common.collect.Sets;
import com.typesafe.config.Config;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;

import static org.testng.Assert.assertEquals;

public class ConfigurationParserTest {
    private File configFile;

    @BeforeMethod
    public void setUp() throws Exception {
        configFile = Files.createTempFile("agent", ".conf").toFile();

        Files.write(configFile.toPath(), "message-buffer-size = 128".getBytes());
    }

    @Test
    public void testParsing() throws Exception{
        final Config config = ConfigurationParser.parse(configFile);

        assertEquals(config.getInt("message-buffer-size"), 128);
    }

    @Test(expectedExceptions = ConfigurationParser.Error.class)
    public void testMissingConfigFile() throws Exception {
        configFile.delete();

        ConfigurationParser.parse(configFile);
    }

    @Test(expectedExceptions = ConfigurationParser.Error.class)
    public void testUnreadableFile() throws Exception {
        Files.setPosixFilePermissions(configFile.toPath(), Sets.newHashSet(new PosixFilePermission[]{}));

        ConfigurationParser.parse(configFile);
    }

    @Test(expectedExceptions = ConfigurationParser.Error.class)
    public void testEmptyFile() throws Exception {
        configFile.delete();
        Files.write(configFile.toPath(), "".getBytes());

        ConfigurationParser.parse(configFile);
    }
}