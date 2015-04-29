package com.graylog.agent.inputs.file;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class FileInputConfigurationValidator implements ConstraintValidator<ValidFileInputConfiguration, FileInputConfiguration> {
    @Override
    public void initialize(ValidFileInputConfiguration constraintAnnotation) {

    }

    @Override
    public boolean isValid(FileInputConfiguration config, ConstraintValidatorContext context) {
        switch (config.getContentSplitter()) {
            case "PATTERN":
                if (config.getContentSplitterPattern() != null && !config.getContentSplitterPattern().isEmpty()) {
                    try {
                        Pattern.compile(config.getContentSplitterPattern(), Pattern.MULTILINE);
                        return true;
                    } catch (PatternSyntaxException ignored) {
                        setMessageTemplate(context, "{com.graylog.agent.inputs.file.ValidFileInputConfiguration.invalidPattern.message}", config.getContentSplitterPattern());
                        return false;
                    }
                }
                setMessageTemplate(context, "{com.graylog.agent.inputs.file.ValidFileInputConfiguration.missingPattern.message}", null);
                return false;
            default:
                return true;
        }
    }

    private void setMessageTemplate(ConstraintValidatorContext context, String messageTemplate, String pattern) {
        HibernateConstraintValidatorContext hibernateContext = context.unwrap(HibernateConstraintValidatorContext.class);
        hibernateContext.disableDefaultConstraintViolation();
        hibernateContext.addExpressionVariable("pattern", pattern).buildConstraintViolationWithTemplate(messageTemplate).addConstraintViolation();
    }
}
