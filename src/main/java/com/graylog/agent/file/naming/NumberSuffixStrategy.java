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
package com.graylog.agent.file.naming;

import java.nio.file.Path;

public class NumberSuffixStrategy implements FileNamingStrategy {

    private final Path basePath;

    public NumberSuffixStrategy(Path basePath) {
        this.basePath = basePath.normalize();
    }

    @Override
    public boolean pathMatches(Path path) {
        path = path.normalize();
        path = basePath.getParent().resolve(path);
        // only allow files in the same directory
        if (!basePath.getParent().equals(path.getParent())) {
            return false;
        }
        final String filename = path.getFileName().toString();
        final String baseFilename = this.basePath.getFileName().toString();

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
