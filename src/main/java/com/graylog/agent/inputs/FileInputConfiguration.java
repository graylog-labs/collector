package com.graylog.agent.inputs;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.graylog.agent.ConfigurationError;

import java.io.File;
import java.util.Collections;
import java.util.List;

public class FileInputConfiguration implements InputConfiguration {
    private final String id;
    private final File path;

    public FileInputConfiguration(final String id, final File path) {
        this.id = id;
        this.path = path;
    }

    public File getPath() {
        return path;
    }

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

    @Override
    public List<ConfigurationError> validate() {
        final List<ConfigurationError> errors = Lists.newArrayList();

        if (Strings.isNullOrEmpty(id)) {
            errors.add(new ConfigurationError("Name is null or empty"));
        }

        if (!path.getParentFile().exists()) {
            errors.add(new ConfigurationError("Directory does not exist: " + path.getParentFile().toString() + " [id=" + getId() + "]"));
        }

        return Collections.unmodifiableList(errors);
    }
}
