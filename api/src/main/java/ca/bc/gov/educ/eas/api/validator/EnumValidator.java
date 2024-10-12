package ca.bc.gov.educ.eas.api.validator;

import ca.bc.gov.educ.eas.api.validator.constraint.IsValidEnum;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.apache.commons.lang3.EnumUtils;

public class EnumValidator implements ConstraintValidator<IsValidEnum, String> {

    private  Class enumClass;

    private EnumValidator() {
    }

    @Override
    public void initialize(IsValidEnum annotation) {
        this.enumClass = annotation.enumClass();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        return EnumUtils.isValidEnum(enumClass, value);
    }
}
