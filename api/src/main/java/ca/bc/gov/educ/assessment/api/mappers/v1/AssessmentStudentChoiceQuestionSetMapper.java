package ca.bc.gov.educ.assessment.api.mappers.v1;

import ca.bc.gov.educ.assessment.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.assessment.api.mappers.UUIDMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentChoiceQuestionSetEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentChoiceQuestionSet;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class, AssessmentStudentChoiceQuestionSetMapper.class})
public interface AssessmentStudentChoiceQuestionSetMapper {
    AssessmentStudentChoiceQuestionSetMapper mapper = Mappers.getMapper(AssessmentStudentChoiceQuestionSetMapper.class);

    @Mapping(target = "assessmentStudentChoiceID", source = "assessmentStudentChoiceEntity.assessmentStudentChoiceID")
    @Mapping(target = "assessmentQuestionID", source = "assessmentQuestionID")
    AssessmentStudentChoiceQuestionSet toStructure(AssessmentStudentChoiceQuestionSetEntity entity);
}
