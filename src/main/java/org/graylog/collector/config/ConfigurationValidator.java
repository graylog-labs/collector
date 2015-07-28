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
package org.graylog.collector.config;

import com.google.common.collect.Lists;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
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
                final String msg = String.format(Locale.getDefault(), "%s (%s)", violation.getMessage(), violation.getPropertyPath().toString());
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
