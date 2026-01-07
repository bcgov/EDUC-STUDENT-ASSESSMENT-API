package ca.bc.gov.educ.assessment.api.mappers.v1;

import ca.bc.gov.educ.assessment.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.assessment.api.mappers.UUIDMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentChoiceEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentChoice;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class, AssessmentStudentChoiceQuestionSetMapper.class})
public interface AssessmentStudentChoiceMapper {
    AssessmentStudentChoiceMapper mapper = Mappers.getMapper(AssessmentStudentChoiceMapper.class);

    @Mapping(target = "assessmentStudentComponentID", source = "assessmentStudentComponentEntity.assessmentStudentComponentID")
    @Mapping(target = "assessmentChoiceID", source = "assessmentChoiceEntity.assessmentChoiceID")
    @Mapping(target = "assessmentStudentChoiceQuestionSet", source = "assessmentStudentChoiceQuestionSetEntities")
    AssessmentStudentChoice toStructure(AssessmentStudentChoiceEntity entity);
}
