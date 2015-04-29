package com.graylog.agent.inputs.file;

import com.typesafe.config.Config;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

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