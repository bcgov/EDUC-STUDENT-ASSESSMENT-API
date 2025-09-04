package ca.bc.gov.educ.assessment.api.mappers.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentComponentEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentComponent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AssessmentComponentMapperDecorator implements AssessmentComponentMapper {
    private final AssessmentComponentMapper delegate;

    protected AssessmentComponentMapperDecorator(AssessmentComponentMapper delegate) {
        this.delegate = delegate;
    }

    @Override
    public AssessmentComponent toStructure(AssessmentComponentEntity entity) {
        return this.delegate.toStructure(entity);
    }
}
