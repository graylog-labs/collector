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

import java.nio.file.Path;

/**
 * Represents a message string extracted from a {@link FileChunkBuffer} via a {@link org.graylog.collector.file.splitters.ContentSplitter}.
 * It includes the offset and the length of the raw message in the file. The {@link #getMessageString()} content is NOT
 * the raw message because LF/CRLF character might have been stripped.
 *
 * The offsets are useful to map back to the location in the file.
 */
public class FileChunkMessage {
    private final String messageString;
    private final long fileOffset;
    private final Path path;
    private final long rawMessageLength;

    /**
     * Creates a new file chunk message.
     *
     * @param messageString    the extracted message string
     * @param path             the path of the file
     * @param fileOffset       the start offset of the raw message in the file
     * @param rawMessageLength the length of the raw message in the file
     */
    public FileChunkMessage(final String messageString, final Path path, final long fileOffset, final long rawMessageLength) {
        this.messageString = messageString;
        this.path = path;
        this.fileOffset = fileOffset;
        this.rawMessageLength = rawMessageLength;
    }

    /**
     * Returns the extracted message string.
     *
     * @return message string
     */
    public String getMessageString() {
        return messageString;
    }

    /**
     * Returns the path of the file.
     *
     * @return path of the file
     */
    public Path getPath() {
        return path;
    }

    /**
     * Returns the offset of the raw message in the file.
     *
     * @return start offset
     */
    public long getFileOffset() {
        return fileOffset;
    }

    /**
     * Returns the length of the raw message.
     *
     * @return raw message length
     */
    public long getRawMessageLength() {
        return rawMessageLength;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("messageString", messageString)
                .add("fileOffset", fileOffset)
                .add("rawMessageLength", rawMessageLength)
                .toString();
    }
}
