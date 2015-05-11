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

public class ExactFileStrategy implements FileNamingStrategy {

    private final Path basePath;

    public ExactFileStrategy(Path basePath) {
        this.basePath = basePath.normalize();
    }

    @Override
    public boolean pathMatches(Path path) {
        path = path.normalize();
        path = basePath.getParent().resolve(path);

        return basePath.equals(path);
    }
}
