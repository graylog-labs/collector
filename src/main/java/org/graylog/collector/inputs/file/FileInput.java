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
import org.graylog.collector.annotations.CollectorHostName;
import org.graylog.collector.buffer.Buffer;
import org.graylog.collector.config.ConfigurationUtils;
import org.graylog.collector.file.ChunkReader;
import org.graylog.collector.file.FileObserver;
import org.graylog.collector.file.FileReaderService;
import org.graylog.collector.file.PathSet;
import org.graylog.collector.inputs.InputService;

import javax.inject.Inject;
import java.util.Set;

public class FileInput extends InputService {
    public enum InitialReadPosition {
        START,
        END
    }

    public interface Factory extends InputService.Factory<FileInput, FileInputConfiguration> {
        FileInput create(FileInputConfiguration configuration);
    }

    private final FileInputConfiguration configuration;
    private final Buffer buffer;
    private final FileObserver fileObserver;
    private final String collectorHostName;
    private FileReaderService readerService;

    @Inject
    public FileInput(@Assisted FileInputConfiguration inputConfiguration, Buffer buffer, FileObserver fileObserver,
                     @CollectorHostName String collectorHostName) {
        this.configuration = inputConfiguration;
        this.buffer = buffer;
        this.fileObserver = fileObserver;
        this.collectorHostName = collectorHostName;
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
    protected void doStart() {
        // TODO needs to be an absolute path because otherwise the FileObserver does weird things. Investigate what's wrong with it.
        final MessageBuilder messageBuilder = new MessageBuilder()
                .input(getId())
                .outputs(getOutputs())
                .source(collectorHostName)
                .fields(configuration.getMessageFields());
        final PathSet pathSet = configuration.getPathSet();
        readerService = new FileReaderService(
                pathSet,
                configuration.getCharset(),
                InitialReadPosition.END,
                this,
                messageBuilder,
                configuration.createContentSplitter(),
                buffer,
                configuration.getReaderBufferSize(),
                configuration.getReaderInterval(),
                fileObserver
        );

        readerService.startAsync().awaitRunning();
        notifyStarted();
    }

    @Override
    protected void doStop() {
        readerService.stopAsync().awaitTerminated();
        notifyStopped();
    }

    @Override
    public void setReaderFinished(ChunkReader chunkReader) {
        // TODO Check if needed and for what it was used.
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileInput fileInput = (FileInput) o;

        return configuration.equals(fileInput.configuration);

    }

    @Override
    public int hashCode() {
        return configuration.hashCode();
    }

    @Override
    public String toString() {
        return ConfigurationUtils.toString(configuration, this);
    }
}
