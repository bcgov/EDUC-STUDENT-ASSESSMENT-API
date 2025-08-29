package ca.bc.gov.educ.assessment.api.mappers.v1;

import ca.bc.gov.educ.assessment.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.assessment.api.mappers.UUIDMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentQuestionEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentQuestion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
public interface AssessmentQuestionMapper {
    AssessmentQuestionMapper mapper = Mappers.getMapper(AssessmentQuestionMapper.class);

    @Mapping(target = "assessmentComponentID", source = "assessmentComponentEntity.assessmentComponentID")
    AssessmentQuestion toStructure(AssessmentQuestionEntity entity);
}
