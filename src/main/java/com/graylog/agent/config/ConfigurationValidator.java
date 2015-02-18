package com.graylog.agent.config;

import com.google.common.collect.Lists;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ConfigurationValidator {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    private final List<ConfigurationError> errors = Lists.newArrayList();

    public boolean isValid(Configuration configuration) {
        return doValidate(configuration);
    }

    private boolean doValidate(Object obj) {
        final Set<ConstraintViolation<Object>> constraintViolations = VALIDATOR.validate(obj);

        if (constraintViolations.size() > 0) {
            for (ConstraintViolation<Object> violation : constraintViolations) {
                final String msg = String.format("%s (%s)", violation.getMessage(), violation.getPropertyPath().toString());
                errors.add(new ConfigurationError(msg));
            }

            return false;
        }

        return true;
    }

    public List<ConfigurationError> getErrors() {
        return Collections.unmodifiableList(errors);
    }
}
