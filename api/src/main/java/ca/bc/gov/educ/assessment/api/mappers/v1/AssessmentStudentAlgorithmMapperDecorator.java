package ca.bc.gov.educ.assessment.api.mappers.v1;

import ca.bc.gov.educ.assessment.api.constants.v1.ProvincialSpecialCaseCodes;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.struct.external.gradalgorithmapi.AssessmentStudentForAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public abstract class AssessmentStudentAlgorithmMapperDecorator implements AssessmentStudentAlgorithmMapper {
    private final AssessmentStudentAlgorithmMapper delegate;

    protected AssessmentStudentAlgorithmMapperDecorator(AssessmentStudentAlgorithmMapper delegate) {
        this.delegate = delegate;
    }

    @Override
    public AssessmentStudentForAlgorithm toStructure(AssessmentStudentEntity entity) {
        final var assessmentStudentAlgorithm = this.delegate.toStructure(entity);
        setWroteFlag(entity, assessmentStudentAlgorithm);
        if(entity.getNumberOfAttempts() != null) {
            assessmentStudentAlgorithm.setExceededWriteFlag(entity.getNumberOfAttempts() >= 3);
        } else {
            assessmentStudentAlgorithm.setExceededWriteFlag(false);
        }
        assessmentStudentAlgorithm.setSessionDate(entity.getAssessmentEntity().getAssessmentSessionEntity().getCourseYear() + "/"
            + entity.getAssessmentEntity().getAssessmentSessionEntity().getCourseMonth());
        return assessmentStudentAlgorithm;
    }

    private void setWroteFlag(AssessmentStudentEntity entity, AssessmentStudentForAlgorithm assessmentStudent) {
        boolean hasProficiencyScore = entity.getProficiencyScore() != null;
        boolean hasSpecialCaseCode = StringUtils.isNotBlank(entity.getProvincialSpecialCaseCode()) &&
            (entity.getProvincialSpecialCaseCode().equals(ProvincialSpecialCaseCodes.NOTCOMPLETED.getCode())
                || entity.getProvincialSpecialCaseCode().equals(ProvincialSpecialCaseCodes.DISQUALIFIED.getCode()));

        assessmentStudent.setWroteFlag(hasProficiencyScore || hasSpecialCaseCode);
    }
} 