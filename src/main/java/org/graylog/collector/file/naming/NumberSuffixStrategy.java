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

public class NumberSuffixStrategy implements FileNamingStrategy {

    private final Iterable<Path> basePaths;

    public NumberSuffixStrategy(Path basePath) {
        this(ImmutableSet.of(basePath));
    }

    public NumberSuffixStrategy(Set<Path> basePaths) {
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
                Path normalizedPath = path.normalize();
                normalizedPath = basePath.getParent().resolve(normalizedPath);
                // only allow files in the same directory
                if (!basePath.getParent().equals(normalizedPath.getParent())) {
                    return false;
                }
                final String filename = normalizedPath.getFileName().toString();
                final String baseFilename = basePath.getFileName().toString();

                // same files are a match
                if (filename.equals(baseFilename)) {
                    return true;
                }

                // do the files have a common beginning? if not, they aren't related.
                if (!filename.startsWith(baseFilename)) {
                    return false;
                }

                // check for number suffix
                final String onlySuffix = filename.substring(baseFilename.length());
                return onlySuffix.matches("^\\.\\d+$");
            }
        });
    }
}
