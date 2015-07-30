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
package org.graylog.collector.file.splitters;

import org.graylog.collector.file.FileChunkBuffer;
import org.graylog.collector.file.FileChunkMessage;

import java.nio.charset.Charset;

public abstract class ContentSplitter {

    public abstract Iterable<FileChunkMessage> split(FileChunkBuffer fileChunkBuffer, Charset charset, boolean includeRemainingData);

    public Iterable<FileChunkMessage> split(FileChunkBuffer fileChunkBuffer, Charset charset) {
        return split(fileChunkBuffer, charset, false);
    }

    public Iterable<FileChunkMessage> splitRemaining(FileChunkBuffer fileChunkBuffer, Charset charset) {
        return split(fileChunkBuffer, charset, true);
    }
}
