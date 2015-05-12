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
import com.typesafe.config.Config;
import org.graylog.collector.config.ConfigurationUtils;
import org.graylog.collector.config.constraints.IsAccessible;
import org.graylog.collector.config.constraints.IsOneOf;
import org.graylog.collector.file.splitters.ContentSplitter;
import org.graylog.collector.file.splitters.NewlineChunkSplitter;
import org.graylog.collector.file.splitters.PatternChunkSplitter;
import org.graylog.collector.inputs.InputConfiguration;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

@ValidFileInputConfiguration
public class FileInputConfiguration extends InputConfiguration {

    public interface Factory extends InputConfiguration.Factory<FileInputConfiguration> {
        @Override
        FileInputConfiguration create(String id, Config config);
    }

    @NotNull
    @IsAccessible
    private File path;

    @IsOneOf({"NEWLINE", "PATTERN"})
    private final String contentSplitter;

    @NotNull
    private final String contentSplitterPattern;

    private final FileInput.Factory inputFactory;

    @NotNull
    private final String charsetString;

    @Inject
    public FileInputConfiguration(@Assisted String id,
                                  @Assisted Config config,
                                  FileInput.Factory inputFactory) {
        super(id, config);
        this.inputFactory = inputFactory;

        if (config.hasPath("path")) {
            this.path = new File(config.getString("path"));
        }
        if (config.hasPath("content-splitter")) {
            this.contentSplitter = config.getString("content-splitter").toUpperCase();

        } else {
            this.contentSplitter = "NEWLINE";
        }
        if (config.hasPath("content-splitter-pattern")) {
            this.contentSplitterPattern = config.getString("content-splitter-pattern");
        } else {
            this.contentSplitterPattern = "";
        }
        if (config.hasPath("charset")) {
            this.charsetString = config.getString("charset");
        } else {
            this.charsetString = "UTF-8";
        }
    }

    @Override
    public FileInput createInput() {
        return inputFactory.create(this);
    }

    public File getPath() {
        return path;
    }

    public String getContentSplitter() {
        return contentSplitter;
    }

    public String getContentSplitterPattern() {
        return contentSplitterPattern;
    }

    public ContentSplitter createContentSplitter() {
        switch (contentSplitter) {
            case "NEWLINE":
                return new NewlineChunkSplitter();
            case "PATTERN":
                return new PatternChunkSplitter(contentSplitterPattern);
            default:
                throw new IllegalArgumentException("Unknown content splitter type: " + contentSplitter);
        }
    }

    public String getCharsetString() {
        return charsetString;
    }

    public Charset getCharset() {
        return Charset.forName(charsetString);
    }

    @Override
    public Map<String, String> toStringValues() {
        return Collections.unmodifiableMap(new HashMap<String, String>(super.toStringValues()) {
            {
                put("path", getPath().toString());
                put("charset", getCharset().toString());
                put("content-splitter", getContentSplitter());
                if (!isNullOrEmpty(contentSplitterPattern)) {
                    put("content-splitter-pattern", getContentSplitterPattern());
                }
            }
        });
    }

    @Override
    public String toString() {
        return ConfigurationUtils.toString(this);
    }
}
