/*
 * Copyright 2014 TORCH GmbH
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.graylog.agent.file;

import com.graylog.agent.file.compat.Buffer;
import com.graylog.agent.file.compat.Message;
import com.graylog.agent.file.compat.MessageInput;
import com.graylog.agent.file.splitters.NewlineChunkSplitter;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.LinkedBlockingQueue;

import static com.google.common.base.Charsets.UTF_8;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

public class ChunkProcessorTest extends MultithreadedBaseTest {
    private static final Logger log = LoggerFactory.getLogger(ChunkProcessorTest.class);

    @Test
    public void testSingleBuffer() {
        final Path logFile = Paths.get("/tmp/file"); // this is never actually materialized

        // the Buffer subclass to check the messages that are generated
        final ChunkProcessor processor = setupProcessor(logFile, new Buffer() {
            int messagenumber = 0;

            @Override
            public void insertFailFast(Message message, MessageInput sourceInput) {
                fail("not using fail fast");
            }

            @Override
            public void insertCached(Message message, MessageInput sourceInput) {
                log.debug("Received message {}", message);
                messagenumber++;
                assertEquals(message.getSource(), "test");
                switch (messagenumber) {
                    case 1:
                        assertEquals(message.getMessage(), "some line");
                        break;
                    case 2:
                        assertEquals(message.getMessage(), "another line");
                        break;
                }
            }
        });

        final FileChunk chunk = new FileChunk(logFile, ChannelBuffers.copiedBuffer("some line\nanother line\n", UTF_8), 1);
        processor.process(chunk);
    }

    @Test
    public void testMultipleBuffers() {
        final Path logFile = Paths.get("/tmp/file"); // this is never actually materialized

        final ChunkProcessor processor = setupProcessor(logFile, new Buffer() {
            public int messageNumber = 0;

            @Override
            public void insertFailFast(Message message, MessageInput sourceInput) {
                fail("not using fail fast");
            }

            @Override
            public void insertCached(Message message, MessageInput sourceInput) {
                log.debug("Received message {}", message);
                assertEquals(message.getSource(), "test");
                messageNumber++;
                switch (messageNumber) {
                    case 1:
                        assertEquals(message.getMessage(), "some line with more content");
                        break;
                    case 2:
                        fail("There should be no second message! Message " + message.toString());
                        break;
                }
            }
        });

        ChannelBuffer undelimitedBuffer = ChannelBuffers.copiedBuffer("some line", UTF_8);
        ChannelBuffer secondBufferDelimitedComplete = ChannelBuffers.copiedBuffer(" with more content\n", UTF_8);
        processor.process(new FileChunk(logFile, undelimitedBuffer, 1));
        processor.process(new FileChunk(logFile, secondBufferDelimitedComplete, 2));
    }

    @Test
    public void testMultipleBuffersRemainingDataInLast() {
        final Path logFile = Paths.get("/tmp/somefile");

        final ChunkProcessor processor = setupProcessor(logFile, new Buffer() {
            public int messageNumber = 0;

            @Override
            public void insertFailFast(Message message, MessageInput sourceInput) {
                fail("not using fail fast");
            }

            @Override
            public void insertCached(Message message, MessageInput sourceInput) {
                messageNumber++;
                switch (messageNumber) {
                    case 1:
                        assertEquals(message.getMessage(), "some line with more content");
                        break;
                    case 2:
                        assertEquals(message.getMessage(), "trailing");
                        break;
                }
            }
        });

        ChannelBuffer undelimitedBuffer = ChannelBuffers.copiedBuffer("some line", UTF_8);
        ChannelBuffer secondBufferDelimitedIncomplete = ChannelBuffers.copiedBuffer(" with more content\ntrailing", UTF_8);
        ChannelBuffer onlyNewline = ChannelBuffers.copiedBuffer("\n", UTF_8);
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
            public void insertFailFast(Message message, MessageInput sourceInput) {
                fail("not using fail fast");
            }

            @Override
            public void insertCached(Message message, MessageInput sourceInput) {
                log.debug(String.valueOf(message.getMessage()));
                messageNumber++;
                switch (messageNumber) {
                    case 1:
                        fail("Empty lines should not trigger any message");
                        break;
                }
            }
        });

        ChannelBuffer firstNewline = ChannelBuffers.copiedBuffer("\n", UTF_8);
        ChannelBuffer secondNewline = ChannelBuffers.copiedBuffer("\n", UTF_8);
        ChannelBuffer thirdNewline = ChannelBuffers.copiedBuffer("\n", UTF_8);
        processor.process(new FileChunk(logFile, firstNewline, 1));
        processor.process(new FileChunk(logFile, secondNewline, 2));
        processor.process(new FileChunk(logFile, thirdNewline, 3));
    }

    private ChunkProcessor setupProcessor(Path logFile, Buffer buffer) {
        final LinkedBlockingQueue<FileChunk> chunkQueue = new LinkedBlockingQueue<>();

        MessageInput input = mock(MessageInput.class);

        final ChunkProcessor processor = new ChunkProcessor(buffer, input, chunkQueue, new NewlineChunkSplitter());
        processor.addFileConfig(logFile, "test", true);

        return processor;
    }

}
