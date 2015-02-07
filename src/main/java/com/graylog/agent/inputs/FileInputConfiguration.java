package com.graylog.agent.inputs;

import com.graylog.agent.config.Configuration;
import com.graylog.agent.config.constraints.IsAccessible;
import com.typesafe.config.Config;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.constraints.NotNull;
import java.io.File;

public class FileInputConfiguration implements Configuration {
    @NotBlank
    private final String id;

    @NotNull
    @IsAccessible
    private File path;

    public FileInputConfiguration(String id, Config config) {
        this.id = id;

        if (config.hasPath("path")) {
            this.path = new File(config.getString("path"));
        }
    }

    public File getPath() {
        return path;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return "FileInputConfiguration{" +
                "id='" + id + '\'' +
                ", path=" + path +
                '}';
    }

}
