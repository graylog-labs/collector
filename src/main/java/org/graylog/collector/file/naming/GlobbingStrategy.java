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

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

import static com.google.common.base.Preconditions.checkNotNull;

public class GlobbingStrategy implements FileNamingStrategy {
    private final PathMatcher matcher;

    public GlobbingStrategy(final Path rootPath, final String pattern) {
        this(rootPath.toString(), pattern, FileSystems.getDefault());
    }

    protected GlobbingStrategy(final String rootPathString, final String pattern, final FileSystem fileSystem) {
        final Path rootPath = fileSystem.getPath(checkNotNull(rootPathString)).toAbsolutePath();

        this.matcher = fileSystem.getPathMatcher(buildGlobPattern(fileSystem, rootPath, pattern));
    }

    @Override
    public boolean pathMatches(Path path) {
        return matcher.matches(path);
    }

    private static String buildGlobPattern(final FileSystem fileSystem, final Path rootPath, final String pattern) {
        final String rootPathString;

        if (!rootPath.toString().endsWith(fileSystem.getSeparator())) {
            rootPathString = rootPath.toString() + fileSystem.getSeparator();
        } else {
            rootPathString = rootPath.toString();
        }

        // Escape special backslash character in root path.
        return "glob:" + rootPathString.replace("\\", "\\\\") + pattern;
    }
}
