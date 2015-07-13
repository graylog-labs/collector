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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Set;

public class ExactFileStrategy implements FileNamingStrategy {

    private final Iterable<Path> basePaths;

    public ExactFileStrategy(Path basePath) {
        this(ImmutableSet.of(basePath));
    }

    public ExactFileStrategy(Set<Path> basePaths) {
        this.basePaths = Iterables.transform(basePaths, new Function<Path, Path>() {
            @Nullable
            @Override
            public Path apply(Path path) {
                return path.normalize().toAbsolutePath();
            }
        });
    }

    @Override
    public boolean pathMatches(final Path path) {
        return Iterables.any(basePaths, new Predicate<Path>() {
            @Override
            public boolean apply(@Nullable Path basePath) {
                if (basePath == null) {
                    return false;
                }

                Path normalizedPath = path.normalize();
                normalizedPath = basePath.getParent().resolve(normalizedPath);

                return basePath.equals(normalizedPath);
            }
        });
    }
}
