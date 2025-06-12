package ca.bc.gov.educ.assessment.api.batch.mapper;

import ca.bc.gov.educ.assessment.api.batch.struct.AssessmentKeyDetails;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentQuestionEntity;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
@DecoratedWith(AssessmentKeysBatchFileDecorator.class)
public interface AssessmentKeysBatchFileMapper {
    AssessmentKeysBatchFileMapper mapper = Mappers.getMapper(AssessmentKeysBatchFileMapper.class);

    String EDUC_STUDENT_ASSESSMENT_API = "EDUC_STUDENT_ASSESSMENT_API";

    @Mapping(target = "assessmentQuestionID", ignore = true)
    @Mapping(target = "updateUser", constant = EDUC_STUDENT_ASSESSMENT_API)
    @Mapping(target = "updateDate", expression = "java(java.time.LocalDateTime.now() )")
    @Mapping(target = "createUser", constant = EDUC_STUDENT_ASSESSMENT_API)
    @Mapping(target = "createDate",expression = "java(java.time.LocalDateTime.now() )")
    AssessmentQuestionEntity toKeyEntity(AssessmentKeyDetails studentDetails);

}
