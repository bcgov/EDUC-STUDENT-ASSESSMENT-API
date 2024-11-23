package ca.bc.gov.educ.eas.api.validator;

import ca.bc.gov.educ.eas.api.constants.v1.ProvincialSpecialCaseCodes;
import ca.bc.gov.educ.eas.api.constants.v1.CourseStatusCodes;
import ca.bc.gov.educ.eas.api.validator.constraint.IsAllowedValue;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;


public class EnumValidator implements ConstraintValidator<IsAllowedValue, String> {

    private String enumName;

    private EnumValidator() {
    }

    @Override
    public void initialize(IsAllowedValue annotation) {
        this.enumName = annotation.enumName();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }

        return switch (enumName) {
            case "ProvincialSpecialCaseCodes" -> ProvincialSpecialCaseCodes.findByValue(value).isPresent();
            case "CourseStatusCodes" -> CourseStatusCodes.findByValue(value).isPresent();
            default -> false;
        };
    }
}
