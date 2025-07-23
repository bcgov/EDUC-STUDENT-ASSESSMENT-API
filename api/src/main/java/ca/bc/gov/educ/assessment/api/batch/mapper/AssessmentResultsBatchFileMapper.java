package ca.bc.gov.educ.assessment.api.batch.mapper;

import ca.bc.gov.educ.assessment.api.batch.struct.AssessmentResultDetails;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import static ca.bc.gov.educ.assessment.api.properties.ApplicationProperties.STUDENT_ASSESSMENT_API;

@Mapper
@DecoratedWith(AssessmentResultsBatchFileDecorator.class)
public interface AssessmentResultsBatchFileMapper {
    AssessmentResultsBatchFileMapper mapper = Mappers.getMapper(AssessmentResultsBatchFileMapper.class);

    @Mapping(target = "stagedStudentResultID", ignore = true)
    @Mapping(target = "stagedStudentResultStatus", ignore = true)
    @Mapping(target = "oeMarks", ignore = true)
    @Mapping(target = "mcMarks", ignore = true)
    @Mapping(target = "provincialSpecialCaseCode", ignore = true)
    @Mapping(target = "adaptedAssessmentCode", ignore = true)
    @Mapping(target = "updateUser", constant = STUDENT_ASSESSMENT_API)
    @Mapping(target = "updateDate", expression = "java(java.time.LocalDateTime.now() )")
    @Mapping(target = "createUser", constant = STUDENT_ASSESSMENT_API)
    @Mapping(target = "createDate",expression = "java(java.time.LocalDateTime.now() )")
    StagedStudentResultEntity toStagedStudentResultEntity(AssessmentResultDetails resultDetails, AssessmentEntity assessmentEntity);

}
