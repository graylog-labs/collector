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
package org.graylog.collector.file.naming;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import org.junit.Test;

import java.nio.file.FileSystem;

import static org.assertj.core.api.Assertions.assertThat;

public class GlobbingStrategyTest {
    private FileSystem newUnixFileSystem() {
        return Jimfs.newFileSystem(Configuration.unix());
    }

    private FileSystem newWindowsFileSystem() {
        return Jimfs.newFileSystem(Configuration.windows());
    }

    @Test
    public void testUnixGlobbing() throws Exception {
        final FileSystem fileSystem = newUnixFileSystem();
        final FileNamingStrategy strategy = new GlobbingStrategy("/var/log", "**/*.{log,gz}", fileSystem);

        final String file1 = "/var/log/upstart/graylog-collector.log";
        final String file2 = "/var/log/test/compressed.log.1.gz";
        final String file3 = "/var/log/foo/bar/baz/test.log";
        final String file4 = "/var/log/test.log";

        assertThat(strategy.pathMatches(fileSystem.getPath(file1))).isEqualTo(true);
        assertThat(strategy.pathMatches(fileSystem.getPath(file2))).isEqualTo(true);
        assertThat(strategy.pathMatches(fileSystem.getPath(file3))).isEqualTo(true);
        assertThat(strategy.pathMatches(fileSystem.getPath(file4))).isEqualTo(false);
    }

    @Test
    public void testUnixGlobbinEdgeCase() throws Exception {
        final FileSystem fileSystem = newUnixFileSystem();
        final FileNamingStrategy strategy = new GlobbingStrategy("/var/log/ups?art", "*.{log,gz}", fileSystem);

        final String file1 = "/var/log/ups?art/graylog-collector.log";

        assertThat(strategy.pathMatches(fileSystem.getPath(file1))).isEqualTo(true);
    }

    @Test
    public void testUnixWithoutPattern() throws Exception {
        final FileSystem fileSystem = newUnixFileSystem();
        final FileNamingStrategy strategy = new GlobbingStrategy("/var/log", "syslog", fileSystem);

        assertThat(strategy.pathMatches(fileSystem.getPath("/var/log/syslog"))).isEqualTo(true);
        assertThat(strategy.pathMatches(fileSystem.getPath("/var/log/mail.log"))).isEqualTo(false);
    }

    @Test
    public void testWindowsGlobbing() throws Exception {
        final FileSystem fileSystem = newWindowsFileSystem();
        final FileNamingStrategy strategy = new GlobbingStrategy("C:\\test", "*.{log,gz}", fileSystem);

        assertThat(strategy.pathMatches(fileSystem.getPath("C:\\test\\test.log"))).isEqualTo(true);
        assertThat(strategy.pathMatches(fileSystem.getPath("C:\\test\\test.log.gz"))).isEqualTo(true);
        assertThat(strategy.pathMatches(fileSystem.getPath("C:\\test\\main.dat"))).isEqualTo(false);
        assertThat(strategy.pathMatches(fileSystem.getPath("C:\\test\\bar\\test.log"))).isEqualTo(false);
    }
}