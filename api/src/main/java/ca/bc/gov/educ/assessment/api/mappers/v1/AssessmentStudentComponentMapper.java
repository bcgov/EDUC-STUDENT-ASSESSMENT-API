package ca.bc.gov.educ.assessment.api.mappers.v1;

import ca.bc.gov.educ.assessment.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.assessment.api.mappers.UUIDMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentComponentEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentComponent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class, AssessmentAnswerMapper.class})
public interface AssessmentStudentComponentMapper {
    AssessmentStudentComponentMapper mapper = Mappers.getMapper(AssessmentStudentComponentMapper.class);

    @Mapping(target = "assessmentStudentID", source = "assessmentStudentEntity.assessmentStudentID")
    @Mapping(target = "assessmentAnswers", source = "assessmentStudentAnswerEntities")
    AssessmentStudentComponent toStructure(AssessmentStudentComponentEntity entity);
}
