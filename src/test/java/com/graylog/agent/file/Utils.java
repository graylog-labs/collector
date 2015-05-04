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
package com.graylog.agent.file;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

public class Utils {

    public static class LogFile {

        private final long initialSize;
        private final int maxLineLength;
        private final int lineLengthDeviation;

        private Path path;
        private BufferedOutputStream bos;
        private Random random;

        public LogFile(long initialSize, int maxLineLength, int lineLengthDeviation) throws IOException {
            this.initialSize = initialSize;
            this.maxLineLength = maxLineLength;
            this.lineLengthDeviation = lineLengthDeviation;

            random = new Random();
            createInitialFile();
        }

        private void createInitialFile() throws IOException {
            long size = initialSize;
            path = Files.createTempFile("gl2-", ".log");
            final File file = path.toFile();
            file.deleteOnExit();
            bos = new BufferedOutputStream(new FileOutputStream(file));

            while (size > 0) {
                final int actualLineLength = appendRandomLine();
                size -= actualLineLength; // line and NL char
            }
            flush();
        }

        public int appendRandomLine() throws IOException {
            final RandomString randomString = new RandomString(maxLineLength);
            final int actualLineLength = maxLineLength - random.nextInt(lineLengthDeviation);
            bos.write(randomString.nextString().substring(0, actualLineLength).getBytes());
            bos.write("\n".getBytes());
            return actualLineLength + 1;
        }

        public int appendLine(String line) throws IOException {
            bos.write(line.getBytes());
            bos.write("\n".getBytes());
            return line.length() + 1; // line and NL char
        }

        public void close() throws IOException {
            bos.close();
        }

        public void flush() throws IOException {
            bos.flush();
        }

        public Path getPath() {
            return path;
        }
    }


    // blatantly stolen from http://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string
    public static class RandomString {
        private static final char[] symbols = new char[36];

        static {
            for (int idx = 0; idx < 10; ++idx)
                symbols[idx] = (char) ('0' + idx);
            for (int idx = 10; idx < 36; ++idx)
                symbols[idx] = (char) ('a' + idx - 10);
        }

        private final Random random = new Random();

        private final char[] buf;

        public RandomString(int length) {
            if (length < 1)
                throw new IllegalArgumentException("length < 1: " + length);
            buf = new char[length];
        }

        public String nextString() {
            for (int idx = 0; idx < buf.length; ++idx)
                buf[idx] = symbols[random.nextInt(symbols.length)];
            return new String(buf);
        }
    }
}
