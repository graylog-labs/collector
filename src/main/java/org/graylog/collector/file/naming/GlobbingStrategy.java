package org.graylog.collector.file.naming;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

import static com.google.common.base.Preconditions.checkNotNull;

public class GlobbingStrategy implements FileNamingStrategy {
    private final PathMatcher matcher;

    public GlobbingStrategy(final Path rootPath, final String pattern) {
        this(rootPath.toString(), pattern, FileSystems.getDefault());
    }

    protected GlobbingStrategy(final String rootPathString, final String pattern, final FileSystem fileSystem) {
        final Path rootPath = fileSystem.getPath(checkNotNull(rootPathString)).toAbsolutePath();

        this.matcher = fileSystem.getPathMatcher(buildGlobPattern(fileSystem, rootPath, pattern));
    }

    @Override
    public boolean pathMatches(Path path) {
        return matcher.matches(path);
    }

    private static String buildGlobPattern(final FileSystem fileSystem, final Path rootPath, final String pattern) {
        final String rootPathString;

        if (!rootPath.toString().endsWith(fileSystem.getSeparator())) {
            rootPathString = rootPath.toString() + fileSystem.getSeparator();
        } else {
            rootPathString = rootPath.toString();
        }

        // Escape special backslash character in root path.
        return "glob:" + rootPathString.replace("\\", "\\\\") + pattern;
    }
}
