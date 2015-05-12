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

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import org.graylog.collector.Message;
import org.graylog.collector.MessageBuilder;
import org.graylog.collector.buffer.Buffer;
import org.graylog.collector.file.splitters.ContentSplitter;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class ChunkProcessor extends AbstractExecutionThreadService {

    private static final Logger log = LoggerFactory.getLogger(ChunkProcessor.class);

    private final Buffer buffer;
    private final MessageBuilder messageBuilder;
    private final BlockingQueue<FileChunk> chunkQueue;

    private final ContentSplitter splitter;
    private final Charset charset;

    @Override
    protected void run() throws Exception {
        while (isRunning()) {
            final FileChunk chunk = chunkQueue.poll(500, TimeUnit.MILLISECONDS);
            if (chunk != null) {
                process(chunk);
            }
        }
    }

    private static class ImmutablePair<K, V> {
        final K first;
        final V second;

        public ImmutablePair(K first, V second) {
            this.first = first;
            this.second = second;
        }

        public static <K, V> ImmutablePair<K, V> of(K first, V second) {
            return new ImmutablePair<>(first, second);
        }
    }

    private Map<Path, ByteBuf> buffersPerFile = Maps.newHashMap();
    private Map<Path, ImmutablePair<String, Boolean>> pathConfig = Maps.newHashMap();

    public ChunkProcessor(Buffer buffer, MessageBuilder messageBuilder, BlockingQueue<FileChunk> chunkQueue, ContentSplitter splitter, final Charset charset) {
        this.buffer = buffer;
        this.messageBuilder = messageBuilder;
        this.chunkQueue = chunkQueue;
        this.splitter = splitter;
        this.charset = charset;
    }

    public void addFileConfig(Path path, String source, boolean overrideTimestamp) {
        pathConfig.put(path, ImmutablePair.of(source, overrideTimestamp));
    }

    public void process(FileChunk chunk) {
        final Path path = chunk.getPath();
        final ImmutablePair<String, Boolean> options = pathConfig.get(path);

        if (chunk.isFinalChunk()) {
            // we've reached the EOF and aren't in follow mode
            log.debug("[{}] Processing final chunk.", path);
            final ByteBuf channelBuffer = buffersPerFile.get(path);
            final Iterable<String> messages = splitter.splitRemaining(channelBuffer, charset);

            createMessages(path, messages);

            // nothing more to do.
            stopAsync();
            return;
        }
        log.debug("[{}] Processing {} bytes chunk (pos {})", new Object[]{path, chunk.getChunkBuffer().readableBytes(), chunk.getId()});

        final ByteBuf byteBuf = buffersPerFile.get(path);
        ByteBuf combinedBuffer;
        if (byteBuf == null) {
            combinedBuffer = chunk.getChunkBuffer();
        } else {
            combinedBuffer = Unpooled.wrappedBuffer(byteBuf, chunk.getChunkBuffer());
        }
        buffersPerFile.put(path, combinedBuffer);
        Iterable<String> messages = splitter.split(combinedBuffer, charset, false);

        createMessages(path, messages);
    }

    private void createMessages(Path path, Iterable<String> messages) {
        for (String messageString : messages) {
            if (messageString.isEmpty()) {
                // skip completely empty messages, they contain no useful information
                continue;
            }

            final Message message = messageBuilder.copy()
                    .message(messageString)
                    .timestamp(DateTime.now(DateTimeZone.UTC))
                    .level(Message.Level.INFO)
                    .build();

            message.getFields().put("source_file", path.toFile().getAbsolutePath());

            buffer.insert(message);
        }
    }

}
