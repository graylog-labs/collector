package com.graylog.agent.inputs.file;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FileInputConfigurationValidator.class)
public @interface ValidFileInputConfiguration {
    String message() default "{com.graylog.agent.inputs.file.ValidFileInputConfiguration.message}";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
