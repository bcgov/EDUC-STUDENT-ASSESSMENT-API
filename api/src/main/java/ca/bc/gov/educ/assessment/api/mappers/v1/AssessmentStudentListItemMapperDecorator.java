package ca.bc.gov.educ.assessment.api.mappers.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentHistorySearchEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentHistoryListItem;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentListItem;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AssessmentStudentListItemMapperDecorator implements AssessmentStudentListItemMapper {
    private final AssessmentStudentListItemMapper delegate;

    protected AssessmentStudentListItemMapperDecorator(AssessmentStudentListItemMapper delegate) {
        this.delegate = delegate;
    }

    @Override
    public AssessmentStudentListItem toStructure(AssessmentStudentEntity entity) {
        final var assessmentStudentListItem = this.delegate.toStructure(entity);
        AssessmentStudentMapperUtils.setWroteFlag(entity, assessmentStudentListItem);
        return assessmentStudentListItem;
    }

    @Override
    public AssessmentStudentHistoryListItem toStructure(AssessmentStudentHistorySearchEntity entity) {
        final var assessmentStudentListItem = this.delegate.toStructure(entity);
        AssessmentStudentMapperUtils.setWroteFlag(entity, assessmentStudentListItem);
        return assessmentStudentListItem;
    }
} 