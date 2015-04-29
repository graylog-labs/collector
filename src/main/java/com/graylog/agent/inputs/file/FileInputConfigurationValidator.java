package com.graylog.agent.inputs.file;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class FileInputConfigurationValidator implements ConstraintValidator<ValidFileInputConfiguration, FileInputConfiguration> {
    @Override
    public void initialize(ValidFileInputConfiguration constraintAnnotation) {

    }

    @Override
    public boolean isValid(FileInputConfiguration config, ConstraintValidatorContext context) {
        try {
            Charset.forName(config.getCharsetString());
        } catch (UnsupportedCharsetException e) {
            setMessageTemplate(context, "{com.graylog.agent.inputs.file.ValidFileInputConfiguration.unsupportedCharset.message}", config.getCharsetString());
            return false;
        } catch (IllegalArgumentException e) {
            setMessageTemplate(context, "{com.graylog.agent.inputs.file.ValidFileInputConfiguration.illegalCharset.message}", config.getContentSplitterPattern());
            return false;
        }

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

    private void setMessageTemplate(ConstraintValidatorContext context, String messageTemplate, String value) {
        HibernateConstraintValidatorContext hibernateContext = context.unwrap(HibernateConstraintValidatorContext.class);
        hibernateContext.disableDefaultConstraintViolation();
        hibernateContext.addExpressionVariable("value", value).buildConstraintViolationWithTemplate(messageTemplate).addConstraintViolation();
    }
}
