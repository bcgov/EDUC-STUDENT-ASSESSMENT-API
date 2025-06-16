package ca.bc.gov.educ.assessment.api.validator;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentTypeCodeEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentTypeCodeRepository;
import ca.bc.gov.educ.assessment.api.struct.v1.Assessment;
import ca.bc.gov.educ.assessment.api.util.ValidationUtil;
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
public class AssessmentValidator {

    public static final String ASSESSMENT = "assessment";

    @Getter(AccessLevel.PRIVATE)
    private final AssessmentRepository assessmentRepository;

    @Getter(AccessLevel.PRIVATE)
    private final AssessmentTypeCodeRepository assessmentTypeCodeRepository;

    @Getter(AccessLevel.PRIVATE)
    private final AssessmentSessionRepository assessmentSessionRepository;

    private static final String ASSESSMENT_ID = "assessmentID";

    @Autowired
    AssessmentValidator(AssessmentRepository assessmentRepository, AssessmentTypeCodeRepository assessmentTypeCodeRepository, AssessmentSessionRepository assessmentSessionRepository) {

        this.assessmentRepository = assessmentRepository;
        this.assessmentTypeCodeRepository = assessmentTypeCodeRepository;
        this.assessmentSessionRepository = assessmentSessionRepository;
    }

    public List<FieldError> validatePayload(Assessment assessment, boolean isCreateOperation) {
        final List<FieldError> apiValidationErrors = new ArrayList<>();

        Optional<AssessmentSessionEntity> sessionEntity = assessmentSessionRepository.findById(UUID.fromString(assessment.getSessionID()));
        if(sessionEntity.isEmpty()){
            apiValidationErrors.add(ValidationUtil.createFieldError(ASSESSMENT, "sessionID", assessment.getSessionID(), "Invalid session ID."));
        }

        Optional<AssessmentTypeCodeEntity> assessmentTypeCodeEntity = assessmentTypeCodeRepository.findByAssessmentTypeCode(assessment.getAssessmentTypeCode());
        if (assessmentTypeCodeEntity.isEmpty() || (sessionEntity.isPresent() && assessmentTypeCodeEntity.get().getExpiryDate().isBefore(sessionEntity.get().getActiveUntilDate())) || (sessionEntity.isPresent() && assessmentTypeCodeEntity.get().getEffectiveDate().isAfter(sessionEntity.get().getActiveUntilDate()))){
            apiValidationErrors.add(ValidationUtil.createFieldError(ASSESSMENT, "assessmentTypeCode", assessment.getAssessmentTypeCode(), "Invalid assessment type code."));
        }

        if(isCreateOperation){
            apiValidationErrors.addAll(validateCreatePayload(assessment));
        } else {
            apiValidationErrors.addAll(validateUpdatePayload(assessment));
        }

        return apiValidationErrors;
    }

    List<FieldError> validateCreatePayload(Assessment assessment) {
        final List<FieldError> apiValidationErrors = new ArrayList<>();
        if (StringUtils.isNotEmpty(assessment.getAssessmentID())) {
            apiValidationErrors.add(ValidationUtil.createFieldError(ASSESSMENT, ASSESSMENT_ID, assessment.getAssessmentID(), "assessmentID should be null for post operation."));
        }
        return apiValidationErrors;
    }

    List<FieldError> validateUpdatePayload(Assessment assessment) {
        final List<FieldError> apiValidationErrors = new ArrayList<>();
        if (StringUtils.isEmpty(assessment.getAssessmentID())) {
            apiValidationErrors.add(ValidationUtil.createFieldError(ASSESSMENT, ASSESSMENT_ID, null, "Assessment ID cannot be null for put operation."));
        }

        Optional<AssessmentEntity> assessmentEntity = assessmentRepository.findById(UUID.fromString(assessment.getAssessmentID()));
        if (assessmentEntity.isEmpty()) {
            apiValidationErrors.add(ValidationUtil.createFieldError(ASSESSMENT, ASSESSMENT_ID, assessment.getAssessmentID(), "Invalid assessment ID."));
        }

        return apiValidationErrors;
    }

}
