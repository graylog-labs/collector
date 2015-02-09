package com.graylog.agent.file;

import com.graylog.agent.Message;
import com.graylog.agent.MessageBuilder;
import com.graylog.agent.buffer.Buffer;
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
            public void insert(Message message) {
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

            @Override
            public Message remove() {
                return null;
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
            public void insert(Message message) {
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

            @Override
            public Message remove() {
                return null;
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
            public void insert(Message message) {
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

            @Override
            public Message remove() {
                return null;
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

        ChannelBuffer firstNewline = ChannelBuffers.copiedBuffer("\n", UTF_8);
        ChannelBuffer secondNewline = ChannelBuffers.copiedBuffer("\n", UTF_8);
        ChannelBuffer thirdNewline = ChannelBuffers.copiedBuffer("\n", UTF_8);
        processor.process(new FileChunk(logFile, firstNewline, 1));
        processor.process(new FileChunk(logFile, secondNewline, 2));
        processor.process(new FileChunk(logFile, thirdNewline, 3));
    }

    private ChunkProcessor setupProcessor(Path logFile, Buffer buffer) {
        final LinkedBlockingQueue<FileChunk> chunkQueue = new LinkedBlockingQueue<>();

        final ChunkProcessor processor = new ChunkProcessor(buffer, new MessageBuilder(), chunkQueue, new NewlineChunkSplitter());
        processor.addFileConfig(logFile, "test", true);

        return processor;
    }

}
