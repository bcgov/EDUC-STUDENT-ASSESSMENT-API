package ca.bc.gov.educ.assessment.api.batch.mapper;

import ca.bc.gov.educ.assessment.api.batch.struct.AssessmentKeyDetails;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentComponentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentFormEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentQuestionEntity;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import static ca.bc.gov.educ.assessment.api.properties.ApplicationProperties.STUDENT_ASSESSMENT_API;

@Mapper
@DecoratedWith(AssessmentKeysBatchFileDecorator.class)
public interface AssessmentKeysBatchFileMapper {
    AssessmentKeysBatchFileMapper mapper = Mappers.getMapper(AssessmentKeysBatchFileMapper.class);

    @Mapping(target = "assessmentFormID", ignore = true)
    @Mapping(target = "updateUser", constant = STUDENT_ASSESSMENT_API)
    @Mapping(target = "updateDate", expression = "java(java.time.LocalDateTime.now() )")
    @Mapping(target = "createUser", constant = STUDENT_ASSESSMENT_API)
    @Mapping(target = "createDate",expression = "java(java.time.LocalDateTime.now() )")
    AssessmentFormEntity toFormEntity(String formCode, AssessmentEntity assessmentEntity);

    @Mapping(target = "assessmentQuestionID", ignore = true)
    @Mapping(target = "itemNumber", ignore = true)
    @Mapping(target = "maxQuestionValue", ignore = true)
    @Mapping(target = "questionNumber", ignore = true)
    @Mapping(target = "masterQuestionNumber", ignore = true)
    @Mapping(target = "scaleFactor", ignore = true)
    @Mapping(target = "updateUser", constant = STUDENT_ASSESSMENT_API)
    @Mapping(target = "updateDate", expression = "java(java.time.LocalDateTime.now() )")
    @Mapping(target = "createUser", constant = STUDENT_ASSESSMENT_API)
    @Mapping(target = "createDate",expression = "java(java.time.LocalDateTime.now() )")
    AssessmentQuestionEntity toQuestionEntity(AssessmentKeyDetails studentDetails, AssessmentComponentEntity componentEntity);

}
