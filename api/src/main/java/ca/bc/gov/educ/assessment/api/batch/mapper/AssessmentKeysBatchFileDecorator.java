package ca.bc.gov.educ.assessment.api.batch.mapper;

import ca.bc.gov.educ.assessment.api.batch.struct.AssessmentKeyDetails;
import ca.bc.gov.educ.assessment.api.mappers.StringMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentComponentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentFormEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentQuestionEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentKeyFileUpload;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;

@Slf4j
public abstract class AssessmentKeysBatchFileDecorator implements AssessmentKeysBatchFileMapper {
    private final AssessmentKeysBatchFileMapper delegate;

    protected AssessmentKeysBatchFileDecorator(AssessmentKeysBatchFileMapper delegate) {
        this.delegate = delegate;
    }

    @Override
    public AssessmentFormEntity toFormEntity(String formCode, AssessmentEntity assessmentEntity, AssessmentKeyFileUpload fileUpload) {
        final var entity = this.delegate.toFormEntity(formCode, assessmentEntity, fileUpload);
        entity.setAssessmentEntity(assessmentEntity);
        entity.setFormCode(formCode);
        return entity;
    }

    @Override
    public AssessmentQuestionEntity toQuestionEntity(AssessmentKeyDetails details, AssessmentComponentEntity componentEntity) {
        final var entity = this.delegate.toQuestionEntity(details, componentEntity);
        entity.setAssessmentComponentEntity(componentEntity);
        entity.setCognitiveLevelCode(StringMapper.trimAndUppercase(details.getCognLevel()));
        entity.setTaskCode(StringMapper.trimAndUppercase(details.getTaskCode()));
        entity.setClaimCode(StringMapper.trimAndUppercase(details.getClaimCode()));
        entity.setContextCode(StringMapper.trimAndUppercase(details.getContextCode()));
        entity.setConceptCode(StringMapper.trimAndUppercase(details.getConceptsCode()));
        entity.setAssessmentSection(StringMapper.trimAndUppercase(details.getAssessmentSection()));
        entity.setQuestionValue(StringUtils.isNotBlank(details.getMark()) ? new BigDecimal(details.getMark()) : null);
        entity.setIrtIncrement(BigDecimal.ZERO);
        entity.setPreloadAnswer(StringMapper.trimAndUppercase(details.getAnswer()));
        var scale = StringMapper.trimAndUppercase(details.getScaleFactor());
        entity.setScaleFactor(StringUtils.isNotBlank(scale) ? (int) (Float.parseFloat(scale) * 100) : 0);
        entity.setIrt(0);
        return entity;
    }

}
