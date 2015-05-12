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
package org.graylog.collector.inputs.file;

import com.google.inject.assistedinject.Assisted;
import org.graylog.collector.MessageBuilder;
import org.graylog.collector.buffer.Buffer;
import org.graylog.collector.config.ConfigurationUtils;
import org.graylog.collector.file.ChunkReader;
import org.graylog.collector.file.FileReaderService;
import org.graylog.collector.file.naming.NumberSuffixStrategy;
import org.graylog.collector.inputs.InputService;
import org.graylog.collector.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

public class FileInput extends InputService {
    public enum InitialReadPosition {
        START,
        END
    }

    public interface Factory extends InputService.Factory<FileInput, FileInputConfiguration> {
        FileInput create(FileInputConfiguration configuration);
    }

    private static final Logger LOG = LoggerFactory.getLogger(FileInput.class);

    private final FileInputConfiguration configuration;
    private final Buffer buffer;
    private final CountDownLatch stopLatch = new CountDownLatch(1);

    @Inject
    public FileInput(@Assisted FileInputConfiguration inputConfiguration, Buffer buffer) {
        this.configuration = inputConfiguration;
        this.buffer = buffer;
    }

    @Override
    public String getId() {
        return configuration.getId();
    }

    @Override
    public Set<String> getOutputs() {
        return configuration.getOutputs();
    }

    @Override
    protected void run() throws Exception {
        final Path path = configuration.getPath().toPath();
        final MessageBuilder messageBuilder = new MessageBuilder().input(getId()).outputs(getOutputs()).source(Utils.getHostname());
        final FileReaderService readerService = new FileReaderService(
                path,
                configuration.getCharset(),
                new NumberSuffixStrategy(path),
                true,
                InitialReadPosition.END,
                this,
                messageBuilder,
                configuration.createContentSplitter(),
                buffer
        );

        readerService.startAsync();
        readerService.awaitRunning();

        stopLatch.await();

        readerService.stopAsync().awaitTerminated();
    }

    @Override
    protected void triggerShutdown() {
        stopLatch.countDown();
    }

    @Override
    public void setReaderFinished(ChunkReader chunkReader) {
        // TODO Check if needed and for what it was used.
    }

    @Override
    public String toString() {
        return ConfigurationUtils.toString(configuration, this);
    }
}
