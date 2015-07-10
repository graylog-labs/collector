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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class SinglePathSet implements PathSet {
    private final Path path;

    public SinglePathSet(final String path) {
        this(path, FileSystems.getDefault());
    }

    public SinglePathSet(final String path, final FileSystem fileSystem) {
        this.path = fileSystem.getPath(checkNotNull(path)).toAbsolutePath();
    }

    @Override
    public Path getRootPath() {
        return path.getParent();
    }

    @Override
    public boolean isInSet(Path path) {
        return path != null && this.path.equals(path.toAbsolutePath());
    }

    @Override
    public Set<Path> getPaths() throws IOException {
        return Files.exists(path) ? ImmutableSet.of(path) : ImmutableSet.<Path>of();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SinglePathSet that = (SinglePathSet) o;

        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this).add("path", path).toString();
    }
}
