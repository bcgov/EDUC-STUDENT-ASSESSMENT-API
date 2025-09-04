package ca.bc.gov.educ.assessment.api.mappers.v1;

import ca.bc.gov.educ.assessment.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.assessment.api.mappers.UUIDMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentAnswerEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentAnswer;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
public interface AssessmentAnswerMapper {
    AssessmentAnswerMapper mapper = Mappers.getMapper(AssessmentAnswerMapper.class);

    @Mapping(target = "assessmentStudentComponentID", source = "assessmentStudentComponentEntity.assessmentStudentComponentID")
    AssessmentAnswer toStructure(AssessmentStudentAnswerEntity entity);
}
