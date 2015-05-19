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

import com.typesafe.config.Config;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileInputConfigurationValidatorTest {
    private static Validator validator;

    @BeforeClass
    public static void setUpClass() throws Exception {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void testConstraint() throws Exception {
        assertEquals(0, validate("pattern", "/\\d+/").size());
        assertEquals(0, validate("newline", null).size());

        assertEquals("Empty pattern should be an error", 1, validate("PATTERN", "").size());
        assertEquals("Null pattern should be an error", 1, validate("PATTERN", null).size());
        assertEquals("Invalid pattern should be an error", 1, validate("PATTERN", "[").size());

        assertEquals("Empty pattern should show the correct message",
                "Missing content splitter pattern.",
                validate("PATTERN", "").iterator().next().getMessage());

        assertEquals("Null pattern should show the correct message",
                "Missing content splitter pattern.",
                validate("PATTERN", null).iterator().next().getMessage());

        assertEquals("Invalid pattern should show the correct message",
                "Invalid content splitter pattern: \"[\"",
                validate("PATTERN", "[").iterator().next().getMessage());
    }

    @Test
    public void testCharset() throws Exception {
        assertEquals(0, validateCharset("utf-8").size());
        assertEquals(0, validateCharset("windows-1252").size());

        assertEquals("Empty charset should be an error", 1, validateCharset("").size());
        assertEquals("Null charset should be an error", 2, validateCharset(null).size());

        assertEquals("Invalid charset should show the correct message",
                "Invalid character set value: \"__foo\"",
                validateCharset("__foo").iterator().next().getMessage());
    }

    @Test
    public void testReaderBufferSize() throws Exception {
        final Config config = mock(Config.class);

        when(config.hasPath("path")).thenReturn(true);
        when(config.getString("path")).thenReturn("target/foo.txt");

        when(config.hasPath("reader-buffer-size")).thenReturn(true);
        when(config.getInt("reader-buffer-size")).thenReturn(1024);

        assertEquals(0, validateConfig(config).size());

        when(config.getInt("reader-buffer-size")).thenReturn(0);

        assertEquals("Invalid buffer size should throw an error", 1, validateConfig(config).size());
        assertEquals("Too small reader buffer should show correct error message",
                "Reader buffer size too small: \"0\"",
                validateConfig(config).iterator().next().getMessage());
    }

    @Test
    public void testReaderInterval() throws Exception {
        final Config config = mock(Config.class);

        when(config.hasPath("path")).thenReturn(true);
        when(config.getString("path")).thenReturn("target/foo.txt");

        when(config.hasPath("reader-interval")).thenReturn(true);
        when(config.getDuration("reader-interval", TimeUnit.MILLISECONDS)).thenReturn(250L);

        assertEquals(0, validateConfig(config).size());

        when(config.getDuration("reader-interval", TimeUnit.MILLISECONDS)).thenReturn(0L);

        assertEquals("Invalid interval should throw an error", 1, validateConfig(config).size());
        assertEquals("Too small reader interval should show correct error message",
                "Reader interval too small: \"0\"",
                validateConfig(config).iterator().next().getMessage());
    }

    private Set<ConstraintViolation<FileInputConfiguration>> validateConfig(Config config) {
        return validator.validate(new FileInputConfiguration("id", config, null));
    }

    private Set<ConstraintViolation<FileInputConfiguration>> validate(String contentSplitter, String contentSplitterPattern) {
        final Config config = mock(Config.class);

        when(config.hasPath("path")).thenReturn(true);
        when(config.getString("path")).thenReturn("target/foo.txt");

        when(config.hasPath("content-splitter")).thenReturn(contentSplitter != null);
        when(config.hasPath("content-splitter-pattern")).thenReturn(contentSplitterPattern != null);
        when(config.getString("content-splitter")).thenReturn(contentSplitter);
        when(config.getString("content-splitter-pattern")).thenReturn(contentSplitterPattern);

        return validator.validate(new FileInputConfiguration("id", config, null));
    }

    private Set<ConstraintViolation<FileInputConfiguration>> validateCharset(String charsetString) {
        final Config config = mock(Config.class);

        when(config.hasPath("path")).thenReturn(true);
        when(config.getString("path")).thenReturn("target/foo.txt");
        when(config.hasPath("content-splitter")).thenReturn(true);
        when(config.getString("content-splitter")).thenReturn("newline");

        when(config.hasPath("charset")).thenReturn(true);
        when(config.getString("charset")).thenReturn(charsetString);

        return validator.validate(new FileInputConfiguration("id", config, null));
    }
}