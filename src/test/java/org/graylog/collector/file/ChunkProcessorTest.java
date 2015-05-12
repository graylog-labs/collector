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

import com.google.common.base.Charsets;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.graylog.collector.Message;
import org.graylog.collector.MessageBuilder;
import org.graylog.collector.buffer.Buffer;
import org.graylog.collector.file.splitters.NewlineChunkSplitter;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.concurrent.LinkedBlockingQueue;

import static com.google.common.base.Charsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ChunkProcessorTest extends MultithreadedBaseTest {
    private static final Logger log = LoggerFactory.getLogger(ChunkProcessorTest.class);

    @Test
    public void testSingleBuffer() {
        final Path logFile = Paths.get("/tmp/file"); // this is never actually materialized

        // the Buffer subclass to check the messages that are generated
        final ChunkProcessor processor = setupProcessor(logFile, new Buffer() {
            int messagenumber = 0;

            @Override
            public void insert(Message message) {
                log.debug("Received message {}", message);
                messagenumber++;
                assertEquals("test", message.getSource());
                switch (messagenumber) {
                    case 1:
                        assertEquals("some line", message.getMessage());
                        break;
                    case 2:
                        assertEquals("another line", message.getMessage());
                        break;
                }
            }

            @Override
            public Message remove() {
                return null;
            }
        });

        final FileChunk chunk = new FileChunk(logFile, Unpooled.copiedBuffer("some line\nanother line\n", UTF_8), 1);
        processor.process(chunk);
    }

    @Test
    public void testMultipleBuffers() {
        final Path logFile = Paths.get("/tmp/file"); // this is never actually materialized

        final ChunkProcessor processor = setupProcessor(logFile, new Buffer() {
            public int messageNumber = 0;

            @Override
            public void insert(Message message) {
                log.debug("Received message {}", message);
                assertEquals("test", message.getSource());
                messageNumber++;
                switch (messageNumber) {
                    case 1:
                        assertEquals("some line with more content", message.getMessage());
                        break;
                    case 2:
                        fail("There should be no second message! Message " + message.toString());
                        break;
                }
            }

            @Override
            public Message remove() {
                return null;
            }
        });

        ByteBuf undelimitedBuffer = Unpooled.copiedBuffer("some line", UTF_8);
        ByteBuf secondBufferDelimitedComplete = Unpooled.copiedBuffer(" with more content\n", UTF_8);
        processor.process(new FileChunk(logFile, undelimitedBuffer, 1));
        processor.process(new FileChunk(logFile, secondBufferDelimitedComplete, 2));
    }

    @Test
    public void testMultipleBuffersRemainingDataInLast() {
        final Path logFile = Paths.get("/tmp/somefile");

        final ChunkProcessor processor = setupProcessor(logFile, new Buffer() {
            public int messageNumber = 0;

            @Override
            public void insert(Message message) {
                messageNumber++;
                switch (messageNumber) {
                    case 1:
                        assertEquals("some line with more content", message.getMessage());
                        break;
                    case 2:
                        assertEquals("trailing", message.getMessage());
                        break;
                }
            }

            @Override
            public Message remove() {
                return null;
            }
        });

        ByteBuf undelimitedBuffer = Unpooled.copiedBuffer("some line", UTF_8);
        ByteBuf secondBufferDelimitedIncomplete = Unpooled.copiedBuffer(" with more content\ntrailing", UTF_8);
        ByteBuf onlyNewline = Unpooled.copiedBuffer("\n", UTF_8);
        processor.process(new FileChunk(logFile, undelimitedBuffer, 1));
        processor.process(new FileChunk(logFile, secondBufferDelimitedIncomplete, 2));
        processor.process(new FileChunk(logFile, onlyNewline, 3)); // this flushes the remaining content in the buffer
    }

    @Test
    public void testEmptyLinesAreIgnored() {
        final Path logFile = Paths.get("/tmp/somefile");

        final ChunkProcessor processor = setupProcessor(logFile, new Buffer() {
            public int messageNumber = 0;

            @Override
            public void insert(Message message) {
                log.debug(String.valueOf(message.getMessage()));
                messageNumber++;
                switch (messageNumber) {
                    case 1:
                        fail("Empty lines should not trigger any message");
                        break;
                }
            }

            @Override
            public Message remove() {
                return null;
            }
        });

        ByteBuf firstNewline = Unpooled.copiedBuffer("\n", UTF_8);
        ByteBuf secondNewline = Unpooled.copiedBuffer("\n", UTF_8);
        ByteBuf thirdNewline = Unpooled.copiedBuffer("\n", UTF_8);
        processor.process(new FileChunk(logFile, firstNewline, 1));
        processor.process(new FileChunk(logFile, secondNewline, 2));
        processor.process(new FileChunk(logFile, thirdNewline, 3));
    }

    private ChunkProcessor setupProcessor(Path logFile, Buffer buffer) {
        final LinkedBlockingQueue<FileChunk> chunkQueue = new LinkedBlockingQueue<>();

        final MessageBuilder messageBuilder = new MessageBuilder().input("input-id").outputs(new HashSet<String>()).source("test");
        final ChunkProcessor processor = new ChunkProcessor(buffer, messageBuilder, chunkQueue, new NewlineChunkSplitter(), Charsets.UTF_8);
        processor.addFileConfig(logFile, "test", true);

        return processor;
    }

}
