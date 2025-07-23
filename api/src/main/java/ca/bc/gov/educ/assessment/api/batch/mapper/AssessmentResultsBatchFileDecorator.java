package ca.bc.gov.educ.assessment.api.batch.mapper;

import ca.bc.gov.educ.assessment.api.batch.struct.AssessmentResultDetails;
import ca.bc.gov.educ.assessment.api.mappers.StringMapper;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AssessmentResultsBatchFileDecorator implements AssessmentResultsBatchFileMapper {
    private final AssessmentResultsBatchFileMapper delegate;

    protected AssessmentResultsBatchFileDecorator(AssessmentResultsBatchFileMapper delegate) {
        this.delegate = delegate;
    }

    @Override
    public StagedStudentResultEntity toStagedStudentResultEntity(AssessmentResultDetails resultDetails, AssessmentEntity assessmentEntity) {
        final var entity = this.delegate.toStagedStudentResultEntity(resultDetails, assessmentEntity);
        entity.setAssessmentEntity(assessmentEntity);
        entity.setOeMarks(StringMapper.trimAndUppercase(resultDetails.getOpenEndedMarks()));
        entity.setMcMarks(StringMapper.trimAndUppercase(resultDetails.getMultiChoiceMarks()));
        entity.setProvincialSpecialCaseCode(StringMapper.trimAndUppercase(resultDetails.getSpecialCaseCode()));
        entity.setAdaptedAssessmentCode(StringMapper.trimAndUppercase(resultDetails.getAdaptedAssessmentIndicator()));
        return entity;
    }

}
