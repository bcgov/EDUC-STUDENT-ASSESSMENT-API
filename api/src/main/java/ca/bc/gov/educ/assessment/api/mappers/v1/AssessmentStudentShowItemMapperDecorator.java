package ca.bc.gov.educ.assessment.api.mappers.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentShowItem;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AssessmentStudentShowItemMapperDecorator implements AssessmentStudentShowItemMapper{
    private final AssessmentStudentShowItemMapper delegate;
    private static final AssessmentDetailsMapper assessmentDetailsMapper = AssessmentDetailsMapper.mapper;

    protected AssessmentStudentShowItemMapperDecorator(AssessmentStudentShowItemMapper delegate) {
        this.delegate = delegate;
    }

    @Override
    public AssessmentStudentShowItem toStructure(AssessmentStudentEntity entity) {
        final var assessmentStudentShowItem = this.delegate.toStructure(entity);
        AssessmentStudentMapperUtils.setWroteFlag(entity, assessmentStudentShowItem);

        if (entity.getAssessmentEntity() != null) {
            assessmentStudentShowItem.setAssessmentDetails(
                assessmentDetailsMapper.toStructure(entity.getAssessmentEntity())
            );
        }

        return assessmentStudentShowItem;
    }
}
