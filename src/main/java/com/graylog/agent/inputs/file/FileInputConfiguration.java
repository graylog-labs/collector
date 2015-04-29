package com.graylog.agent.inputs.file;

import com.google.inject.assistedinject.Assisted;
import com.graylog.agent.config.ConfigurationUtils;
import com.graylog.agent.config.constraints.IsAccessible;
import com.graylog.agent.inputs.InputConfiguration;
import com.typesafe.config.Config;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class FileInputConfiguration extends InputConfiguration {

    public interface Factory extends InputConfiguration.Factory<FileInputConfiguration> {
        @Override
        FileInputConfiguration create(String id, Config config);
    }

    @NotNull
    @IsAccessible
    private File path;

    private final FileInput.Factory inputFactory;

    @Inject
    public FileInputConfiguration(@Assisted String id,
                                  @Assisted Config config,
                                  FileInput.Factory inputFactory) {
        super(id, config);
        this.inputFactory = inputFactory;

        if (config.hasPath("path")) {
            this.path = new File(config.getString("path"));
        }
    }

    @Override
    public FileInput createInput() {
        return inputFactory.create(this);
    }

    public File getPath() {
        return path;
    }

    @Override
    public Map<String, String> toStringValues() {
        return Collections.unmodifiableMap(new HashMap<String, String>(super.toStringValues()) {
            {
                put("path", getPath().toString());
            }
        });
    }

    @Override
    public String toString() {
        return ConfigurationUtils.toString(this);
    }
}
