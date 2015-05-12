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
