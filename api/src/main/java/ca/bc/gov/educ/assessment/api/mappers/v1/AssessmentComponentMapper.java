package ca.bc.gov.educ.assessment.api.mappers.v1;

import ca.bc.gov.educ.assessment.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.assessment.api.mappers.UUIDMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentComponentEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentComponent;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class, AssessmentQuestionMapper.class, AssessmentChoiceMapper.class})
@DecoratedWith(AssessmentComponentMapperDecorator.class)
public interface AssessmentComponentMapper {
    AssessmentComponentMapper mapper = Mappers.getMapper(AssessmentComponentMapper.class);

    @Mapping(target = "assessmentFormID", source = "assessmentFormEntity.assessmentFormID")
    @Mapping(target = "assessmentQuestions", source = "assessmentQuestionEntities")
    @Mapping(target = "assessmentChoices", source = "assessmentChoiceEntities")
    AssessmentComponent toStructure(AssessmentComponentEntity entity);
}
