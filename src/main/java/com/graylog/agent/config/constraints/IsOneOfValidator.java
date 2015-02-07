package com.graylog.agent.config.constraints;

import com.google.common.base.Joiner;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.Arrays;

public class IsOneOfValidator implements ConstraintValidator<IsOneOf, String> {
    private String[] strings;

    @Override
    public void initialize(IsOneOf constraintAnnotation) {
        this.strings = constraintAnnotation.value();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        final boolean valid = Arrays.asList(strings).contains(value);

        if (!valid) {
            HibernateConstraintValidatorContext hibernateContext = context.unwrap(HibernateConstraintValidatorContext.class);
            hibernateContext.disableDefaultConstraintViolation();

            hibernateContext.addExpressionVariable("validValues", Joiner.on(" ").join(strings))
                    .buildConstraintViolationWithTemplate(hibernateContext.getDefaultConstraintMessageTemplate())
                    .addConstraintViolation();
        }

        return valid;
    }
}
