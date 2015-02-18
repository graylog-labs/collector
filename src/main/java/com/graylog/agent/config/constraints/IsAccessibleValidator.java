package com.graylog.agent.config.constraints;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.io.File;

public class IsAccessibleValidator implements ConstraintValidator<IsAccessible, File> {
    @Override
    public void initialize(IsAccessible constraintAnnotation) {
    }

    @Override
    public boolean isValid(File file, ConstraintValidatorContext context) {
        if (file == null) {
            return true;
        }

        if (!file.getParentFile().exists() || !file.getParentFile().canRead()) {
            buildMessage(file, context);
            return false;
        }

        if (file.exists() && !file.canRead()) {
            buildMessage(file, context);
            return false;
        }


        return true;
    }

    private void buildMessage(File file, ConstraintValidatorContext context) {
        HibernateConstraintValidatorContext hibernateContext = context.unwrap(HibernateConstraintValidatorContext.class);
        hibernateContext.disableDefaultConstraintViolation();

        hibernateContext.addExpressionVariable("theFile", file.toString())
                .addExpressionVariable("theDir", file.getParentFile().toString())
                .buildConstraintViolationWithTemplate(hibernateContext.getDefaultConstraintMessageTemplate())
                .addConstraintViolation();
    }
}
