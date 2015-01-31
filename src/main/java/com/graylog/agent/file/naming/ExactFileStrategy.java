package com.graylog.agent.file.naming;

import java.nio.file.Path;

public class ExactFileStrategy implements FileNamingStrategy {

    private final Path basePath;

    public ExactFileStrategy(Path basePath) {
        this.basePath = basePath.normalize();
    }

    @Override
    public boolean pathMatches(Path path) {
        path = path.normalize();
        path = basePath.getParent().resolve(path);

        return basePath.equals(path);
    }
}
