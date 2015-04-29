package com.graylog.agent.inputs.file;

import com.google.inject.assistedinject.Assisted;
import com.graylog.agent.config.ConfigurationUtils;
import com.graylog.agent.config.constraints.IsAccessible;
import com.graylog.agent.config.constraints.IsOneOf;
import com.graylog.agent.file.splitters.ContentSplitter;
import com.graylog.agent.file.splitters.NewlineChunkSplitter;
import com.graylog.agent.file.splitters.PatternChunkSplitter;
import com.graylog.agent.inputs.InputConfiguration;
import com.typesafe.config.Config;

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

    @IsOneOf({"NEWLINE", "CRLF", "PATTERN"})
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
            this.contentSplitter = "PATTERN";
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
        switch(contentSplitter) {
            case "NEWLINE":
                return new NewlineChunkSplitter(NewlineChunkSplitter.LineEnding.LF);
            case "CRLF":
                return new NewlineChunkSplitter(NewlineChunkSplitter.LineEnding.CRLF);
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
