package ca.bc.gov.educ.assessment.api.batch.mapper;

import ca.bc.gov.educ.assessment.api.batch.struct.AssessmentKeyDetails;
import ca.bc.gov.educ.assessment.api.mappers.StringMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentQuestionEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public abstract class AssessmentKeysBatchFileDecorator implements AssessmentKeysBatchFileMapper {
    private final AssessmentKeysBatchFileMapper delegate;

    protected AssessmentKeysBatchFileDecorator(AssessmentKeysBatchFileMapper delegate) {
        this.delegate = delegate;
    }


    @Override
    public AssessmentQuestionEntity toKeyEntity(AssessmentKeyDetails details) {
        final var entity = this.delegate.toKeyEntity(details);
        //formId
        entity.setQuestionNumber(StringUtils.isNotBlank(details.getQuestionNumber()) ? Integer.parseInt(details.getQuestionNumber()) : null); // add thePK/FK relationship
//        entity.setItemType(details.getItemType());

        entity.setCognitiveLevelCode(StringMapper.trimAndUppercase(details.getCognLevel()));
        entity.setTaskCode(StringMapper.trimAndUppercase(details.getTaskCode()));
        entity.setClaimCode(StringMapper.trimAndUppercase(details.getClaimCode()));
        entity.setContextCode(StringMapper.trimAndUppercase(details.getContextCode()));
        entity.setConceptCode(StringMapper.trimAndUppercase(details.getConceptsCode()));
        entity.setAssessmentSection(StringMapper.trimAndUppercase(details.getAssessmentSection()));
//        entity.setMcOeFlag(StringMapper.trimAndUppercase());
//        entity.setItemNumber(StringMapper.trimAndUppercase());
//        entity.setQuestionValue(StringMapper.trimAndUppercase());
//        entity.setMaxQuestionValue(StringMapper.trimAndUppercase(details));
//        entity.setMasterQuestionNumber(StringMapper.trimAndUppercase(studentDetails.getCity()));
//        entity.setIrtIncrement(StringMapper.trimAndUppercase());
        entity.setPreloadAnswer(StringMapper.trimAndUppercase(details.getAnswer()));
        entity.setIrt(StringUtils.isNotBlank(details.getIrt()) ? Integer.parseInt(details.getIrt()) : null);
        //mark, topicType, topicType, questionOrigin, item
        return entity;
    }

}
