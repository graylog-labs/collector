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

import com.google.inject.Scopes;
import org.graylog.collector.guice.CollectorModule;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.WatchService;

public class FileModule extends CollectorModule {
    @Override
    protected void configure() {
        try {
            bind(WatchService.class).toInstance(FileSystems.getDefault().newWatchService());
        } catch (IOException e) {
            throw new RuntimeException("Unable to create WatchService");
        }

        bind(FileObserver.class).in(Scopes.SINGLETON);
        registerService(FileObserver.class);
    }
}
