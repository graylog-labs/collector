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

import java.nio.file.Path;

public class NumberSuffixStrategy implements FileNamingStrategy {

    private final Path path;

    public NumberSuffixStrategy(Path path) {
        this.path = path;
    }

    @Override
    public boolean pathMatches(final Path path) {
        if (this.path == null) {
            return false;
        }

        Path normalizedPath = path.normalize();
        normalizedPath = this.path.getParent().resolve(normalizedPath);
        // only allow files in the same directory
        if (!this.path.getParent().equals(normalizedPath.getParent())) {
            return false;
        }
        final String filename = normalizedPath.getFileName().toString();
        final String baseFilename = this.path.getFileName().toString();

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
}
