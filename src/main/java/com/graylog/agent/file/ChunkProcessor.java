package com.graylog.agent.file;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.graylog.agent.file.compat.Buffer;
import com.graylog.agent.file.compat.Message;
import com.graylog.agent.file.compat.MessageInput;
import com.graylog.agent.file.splitters.ContentSplitter;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class ChunkProcessor extends AbstractExecutionThreadService {

    private static final Logger log = LoggerFactory.getLogger(ChunkProcessor.class);

    private final Buffer buffer;
    private final MessageInput input;
    private final BlockingQueue<FileChunk> chunkQueue;
    private String hostname = "";

    private final ContentSplitter splitter;

    @Override
    protected void run() throws Exception {
        while (isRunning()) {
            final FileChunk chunk = chunkQueue.poll(500, TimeUnit.MILLISECONDS);
            if (chunk != null) {
                process(chunk);
            }
        }
    }

    private static class ImmutablePair<K,V> {
        final K first;
        final V second;

        public ImmutablePair(K first, V second) {
            this.first = first;
            this.second = second;
        }

        public static <K, V> ImmutablePair<K,V> of(K first, V second) {
            return new ImmutablePair<>(first, second);
        }
    }

    private Map<Path, ChannelBuffer> buffersPerFile = Maps.newHashMap();
    private Map<Path, ImmutablePair<String, Boolean>> pathConfig = Maps.newHashMap();

    public ChunkProcessor(Buffer buffer, MessageInput input, BlockingQueue<FileChunk> chunkQueue, ContentSplitter splitter) {
        this.buffer = buffer;
        this.input = input;
        this.chunkQueue = chunkQueue;
        this.splitter = splitter;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = System.getenv("HOSTNAME");
            if (hostname == null) {
                hostname = System.getenv("COMPUTERNAME");
            }
            if (hostname == null) {
                hostname = "unknown host";
                log.warn("Unable to detect the local host name, use source override!");
            }
        }
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
            final ChannelBuffer channelBuffer = buffersPerFile.get(path);
            final Iterable<String> messages = splitter.splitRemaining(channelBuffer);

            createMessages(options, messages);

            // nothing more to do.
            stopAsync();
            return;
        }
        log.debug("[{}] Processing {} bytes chunk (pos {})", new Object[] {path, chunk.getChunkBuffer().readableBytes(), chunk.getId()});

        final ChannelBuffer channelBuffer = buffersPerFile.get(path);
        ChannelBuffer combinedBuffer;
        if (channelBuffer == null) {
            combinedBuffer = chunk.getChunkBuffer();
        } else {
            combinedBuffer = ChannelBuffers.wrappedBuffer(channelBuffer, chunk.getChunkBuffer());
        }
        buffersPerFile.put(path, combinedBuffer);
        Iterable<String> messages = splitter.split(combinedBuffer);

        createMessages(options, messages);
    }

    private void createMessages(ImmutablePair<String, Boolean> options, Iterable<String> messages) {
        for (String msgSource : messages) {
            if (msgSource.isEmpty()) {
                // skip completely empty messages, they contain no useful information
                continue;
            }
            final Message message = new Message(msgSource,
                                                Strings.isNullOrEmpty(options.first) ? hostname : options.first,
                                                DateTime.now(DateTimeZone.UTC));
            buffer.insert(message, input);
        }
    }

}
