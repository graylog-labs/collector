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
            setMessageTemplate(context, "{org.graylog.collector.inputs.file.ValidFileInputConfiguration.unsupportedCharset.message}", config.getCharsetString());
            return false;
        } catch (IllegalArgumentException e) {
            setMessageTemplate(context, "{org.graylog.collector.inputs.file.ValidFileInputConfiguration.illegalCharset.message}", config.getCharsetString());
            return false;
        }

        switch (config.getContentSplitter()) {
            case "PATTERN":
                if (config.getContentSplitterPattern() != null && !config.getContentSplitterPattern().isEmpty()) {
                    try {
                        Pattern.compile(config.getContentSplitterPattern(), Pattern.MULTILINE);
                        return true;
                    } catch (PatternSyntaxException ignored) {
                        setMessageTemplate(context, "{org.graylog.collector.inputs.file.ValidFileInputConfiguration.invalidPattern.message}", config.getContentSplitterPattern());
                        return false;
                    }
                }
                setMessageTemplate(context, "{org.graylog.collector.inputs.file.ValidFileInputConfiguration.missingPattern.message}", null);
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
