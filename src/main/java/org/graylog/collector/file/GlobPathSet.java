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
package org.graylog.collector.file;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class GlobPathSet implements PathSet {
    private static final Logger LOG = LoggerFactory.getLogger(GlobPathSet.class);

    private final String pattern;
    private final FileTreeWalker fileTreeWalker;
    private final PathMatcher matcher;
    private final Path rootPath;

    public static class GlobbingFileVisitor extends SimpleFileVisitor<Path> {
        private final PathMatcher matcher;
        private final ImmutableSet.Builder<Path> matchedPaths;

        public GlobbingFileVisitor(PathMatcher matcher, ImmutableSet.Builder<Path> matchedPaths) {
            this.matcher = matcher;
            this.matchedPaths = matchedPaths;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            // Skip /proc because it can throw some permission errors we cannot check for.
            return "/proc".equals(dir.toString()) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
            LOG.warn("Unable to change into directory {} - Check permissions", file);
            return FileVisitResult.SKIP_SUBTREE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
            if (matcher.matches(file)) {
                // TODO needs to be an absolute path because otherwise the FileObserver does weird things. Investigate what's wrong with it.
                matchedPaths.add(file.toAbsolutePath());
            }

            return FileVisitResult.CONTINUE;
        }
    }

    public interface FileTreeWalker {
        void walk(Path basePath, FileVisitor<Path> visitor) throws IOException;
    }

    public GlobPathSet(final String rootPathString, final String pattern) {
        this(rootPathString, pattern, new FileTreeWalker() {
            @Override
            public void walk(Path basePath, FileVisitor<Path> visitor) throws IOException {
                Files.walkFileTree(basePath, visitor);
            }
        });
    }

    public GlobPathSet(final String rootPathString, final String pattern, final FileTreeWalker fileTreeWalker) {
        this(rootPathString, pattern, fileTreeWalker, FileSystems.getDefault());
    }

    public GlobPathSet(final String rootPathString, final String pattern, final FileTreeWalker fileTreeWalker, final FileSystem fileSystem) {
        this.rootPath = fileSystem.getPath(checkNotNull(rootPathString)).toAbsolutePath();
        this.pattern = pattern;
        this.fileTreeWalker = fileTreeWalker;
        this.matcher = fileSystem.getPathMatcher(buildGlobPattern(fileSystem, rootPath, pattern));
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

    @Override
    public Path getRootPath() {
        return rootPath;
    }

    @Override
    public boolean isInSet(Path path) {
        return matcher.matches(path);
    }

    /**
     * Returns a {@link Set<Path>} of all existing paths that match the pattern.
     *
     * The file tree is walked on every invocation to pick up newly created paths. This can be expensive!
     *
     * @return all existing paths that match the pattern
     * @throws IOException
     */
    @Override
    public Set<Path> getPaths() throws IOException {
        final ImmutableSet.Builder<Path> matchedPaths = ImmutableSet.builder();

        fileTreeWalker.walk(rootPath, new GlobbingFileVisitor(matcher, matchedPaths));

        return matchedPaths.build();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GlobPathSet that = (GlobPathSet) o;

        return pattern.equals(that.pattern) && rootPath.equals(that.rootPath);

    }

    @Override
    public int hashCode() {
        int result = pattern.hashCode();
        result = 31 * result + rootPath.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("rootPath", rootPath)
                .add("pattern", pattern)
                .toString();
    }
}
