package com.graylog.agent.config.constraints;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static org.testng.Assert.assertEquals;

public class IsOneOfValidatorTest {
    private static class TestObject {
        @IsOneOf({"hello", "world"})
        private final String theValue;

        public TestObject(String theValue) {
            this.theValue = theValue;
        }
    }

    private static Validator validator;

    @BeforeClass
    public void setUpClass() throws Exception {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    public void testConstraint() throws Exception {
        assertEquals(validate("hello").size(), 0);
        assertEquals(validate("world").size(), 0);
        assertEquals(validate("wat?").size(), 1);
        assertEquals(validate("wat?").iterator().next().getMessage(), "\"wat?\" is not one of: hello world");

        // Check different case.
        assertEquals(validate("Hello").size(), 1);
        assertEquals(validate("WORLD").size(), 1);
    }

    private Set<ConstraintViolation<TestObject>> validate(String value) {
        return validator.validate(new TestObject(value));
    }
}