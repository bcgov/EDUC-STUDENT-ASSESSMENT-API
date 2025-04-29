package ca.bc.gov.educ.assessment.api.validator.constraint;

import ca.bc.gov.educ.assessment.api.validator.EnumValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EnumValidator.class)
public @interface IsAllowedValue {
    String message() default "Invalid value";

    String enumName();

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
