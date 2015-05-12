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
package org.graylog.collector.config.constraints;

import org.junit.BeforeClass;
import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class IsAccessibleValidatorTest {
    private static class TestObject {
        @IsAccessible
        private final File file;

        public TestObject(File file) {
            this.file = file;
        }
    }

    private static Validator validator;

    @BeforeClass
    public static void setUpClass() throws Exception {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void testConstraint() throws Exception {
        final Path path = Files.createTempDirectory("file-constraint-test");
        final File file = File.createTempFile("is-accessible", "constraint", path.toFile());

        assertEquals(0, validate(file).size());

        // Delete temp file and the temp directory.
        file.delete();
        path.toFile().delete();

        assertEquals(1, validate(file).size());
        assertEquals(file.toString() + " or " + path.toString() + " is not accessible (check if directory exists and permissions are correct)", validate(file).iterator().next().getMessage());
    }

    private Set<ConstraintViolation<TestObject>> validate(File value) {
        return validator.validate(new TestObject(value));
    }
}