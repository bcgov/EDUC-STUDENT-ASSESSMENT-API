package ca.bc.gov.educ.assessment.api.mappers.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AssessmentStudentMapperDecorator implements AssessmentStudentMapper {
    private final AssessmentStudentMapper delegate;

    protected AssessmentStudentMapperDecorator(AssessmentStudentMapper delegate) {
        this.delegate = delegate;
    }

    @Override
    public AssessmentStudent toStructure(AssessmentStudentEntity entity) {
        final var assessmentStudent = this.delegate.toStructure(entity);
        AssessmentStudentMapperUtils.setWroteFlag(entity, assessmentStudent);
        return assessmentStudent;
    }
} 