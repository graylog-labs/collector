package com.graylog.agent.config.constraints;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = IsOneOfValidator.class)
public @interface IsOneOf {
    String message() default "{com.graylog.agent.config.constraints.IsOneOf.message}";

    String[] value();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
