package ca.bc.gov.educ.eas.api.validator;

import ca.bc.gov.educ.eas.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.eas.api.repository.v1.AssessmentRepository;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.eas.api.util.PenUtil;
import ca.bc.gov.educ.eas.api.util.ValidationUtil;
import lombok.AccessLevel;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.FieldError;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Component
public class AssessmentStudentValidator {

    public static final String ASSESSMENT_STUDENT = "assessmentStudent";

    @Getter(AccessLevel.PRIVATE)
    private final AssessmentRepository assessmentRepository;

    @Autowired
    AssessmentStudentValidator(AssessmentRepository assessmentRepository) {
        this.assessmentRepository = assessmentRepository;
    }

    public List<FieldError> validatePayload(AssessmentStudent assessmentStudent, boolean isCreateOperation) {
        final List<FieldError> apiValidationErrors = new ArrayList<>();

        if (StringUtils.isNotEmpty(assessmentStudent.getPen()) && !PenUtil.validCheckDigit(assessmentStudent.getPen())) {
            apiValidationErrors.add(ValidationUtil.createFieldError(ASSESSMENT_STUDENT, "pen", assessmentStudent.getPen(), "Invalid Student Pen."));
        }
        Optional<AssessmentEntity> assessmentEntity = assessmentRepository.findById(UUID.fromString(assessmentStudent.getAssessmentID()));
        if (assessmentEntity.isEmpty()) {
            apiValidationErrors.add(ValidationUtil.createFieldError(ASSESSMENT_STUDENT, "assessmentID", assessmentStudent.getAssessmentID(), "Invalid assessment session."));
        }

        if (isCreateOperation) {
            apiValidationErrors.addAll(validateCreatePayload(assessmentStudent));
        } else {
            apiValidationErrors.addAll(validateUpdatePayload(assessmentStudent));
        }
        return apiValidationErrors;
    }

    List<FieldError> validateCreatePayload(AssessmentStudent assessmentStudent) {
        final List<FieldError> apiValidationErrors = new ArrayList<>();
        if (StringUtils.isNotEmpty(assessmentStudent.getAssessmentStudentID())) {
            apiValidationErrors.add(ValidationUtil.createFieldError(ASSESSMENT_STUDENT, "assessmentStudentID", assessmentStudent.getAssessmentStudentID(), "assessmentStudentID should be null for post operation."));
        }
        return apiValidationErrors;
    }

    List<FieldError> validateUpdatePayload(AssessmentStudent assessmentStudent) {
        final List<FieldError> apiValidationErrors = new ArrayList<>();
        if (StringUtils.isEmpty(assessmentStudent.getAssessmentStudentID())) {
            apiValidationErrors.add(ValidationUtil.createFieldError(ASSESSMENT_STUDENT, "assessmentStudentID", null, "assessmentStudentID cannot be null for put operation."));
        }
        return apiValidationErrors;
    }

}
