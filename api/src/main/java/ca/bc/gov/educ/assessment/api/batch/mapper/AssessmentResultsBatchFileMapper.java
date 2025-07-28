package ca.bc.gov.educ.assessment.api.batch.mapper;

import ca.bc.gov.educ.assessment.api.batch.struct.AssessmentResultDetails;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentResultFileUpload;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

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
    @Mapping(target = "updateUser", source = "fileUpload.updateUser")
    @Mapping(target = "updateDate", expression = "java(java.time.LocalDateTime.now() )")
    @Mapping(target = "createUser", source = "fileUpload.createUser")
    @Mapping(target = "createDate",expression = "java(java.time.LocalDateTime.now() )")
    StagedStudentResultEntity toStagedStudentResultEntity(AssessmentResultDetails resultDetails, AssessmentEntity assessmentEntity, AssessmentResultFileUpload fileUpload);

}
