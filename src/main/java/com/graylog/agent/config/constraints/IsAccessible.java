package com.graylog.agent.config.constraints;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IsAccessibleValidator.class)
public @interface IsAccessible {
    String message() default "{com.graylog.agent.config.constraints.IsAccessible.message}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
