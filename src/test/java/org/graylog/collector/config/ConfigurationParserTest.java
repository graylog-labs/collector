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
package org.graylog.collector.config;

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
        configFile = Files.createTempFile("collector", ".conf").toFile();

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