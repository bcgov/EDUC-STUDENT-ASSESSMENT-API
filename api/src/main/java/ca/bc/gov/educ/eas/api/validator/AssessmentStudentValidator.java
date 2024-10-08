package ca.bc.gov.educ.eas.api.validator;

import ca.bc.gov.educ.eas.api.constants.v1.AssessmentTypeCodes;
import ca.bc.gov.educ.eas.api.constants.v1.CourseStatusCodes;
import ca.bc.gov.educ.eas.api.constants.v1.ProvincialSpecialCaseCodes;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.eas.api.util.PenUtil;
import ca.bc.gov.educ.eas.api.util.ValidationUtil;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;

import java.util.ArrayList;
import java.util.List;

@Component
public class AssessmentStudentValidator {

    public static final String ASSESSMENT_STUDENT = "assessmentStudent";
    public List<FieldError> validatePayload(AssessmentStudent assessmentStudent, boolean isCreateOperation) {
        final List<FieldError> apiValidationErrors = new ArrayList<>();

        if(isCreateOperation && StringUtils.isNotEmpty(assessmentStudent.getAssessmentStudentID())) {
            apiValidationErrors.add(ValidationUtil.createFieldError(ASSESSMENT_STUDENT, "assessmentStudentID", assessmentStudent.getAssessmentStudentID(), "assessmentStudentID should be null for post operation."));
        }
        if (!isCreateOperation && StringUtils.isEmpty(assessmentStudent.getAssessmentStudentID())) {
            apiValidationErrors.add(ValidationUtil.createFieldError(ASSESSMENT_STUDENT, "assessmentStudentID", null, "assessmentStudentID cannot be null for put operation."));
        }
        if (StringUtils.isNotEmpty(assessmentStudent.getPen()) && !PenUtil.validCheckDigit(assessmentStudent.getPen())) {
            apiValidationErrors.add(ValidationUtil.createFieldError(ASSESSMENT_STUDENT, "pen", assessmentStudent.getPen(), "Invalid Student Pen."));
        }
        if (!EnumUtils.isValidEnum(AssessmentTypeCodes.class, assessmentStudent.getAssessmentTypeCode())) {
            apiValidationErrors.add(ValidationUtil.createFieldError(ASSESSMENT_STUDENT, "assessmentTypeCode", assessmentStudent.getAssessmentTypeCode(), "Invalid assessment type code."));
        }
        if (assessmentStudent.getProvincialSpecialCaseCode() != null && !EnumUtils.isValidEnum(ProvincialSpecialCaseCodes.class, assessmentStudent.getProvincialSpecialCaseCode())) {
            apiValidationErrors.add(ValidationUtil.createFieldError(ASSESSMENT_STUDENT, "provincialSpecialCaseCode", assessmentStudent.getProvincialSpecialCaseCode(), "Invalid provincial special case code."));
        }
        if (assessmentStudent.getCourseStatusCode() != null && !EnumUtils.isValidEnum(CourseStatusCodes.class, assessmentStudent.getCourseStatusCode())) {
            apiValidationErrors.add(ValidationUtil.createFieldError(ASSESSMENT_STUDENT, "courseStatusCode", assessmentStudent.getCourseStatusCode(), "Invalid course status code."));
        }
        return apiValidationErrors;
    }
}
