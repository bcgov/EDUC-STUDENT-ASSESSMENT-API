package ca.bc.gov.educ.assessment.api.mappers.v1;

import ca.bc.gov.educ.assessment.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.assessment.api.mappers.UUIDMapper;
import ca.bc.gov.educ.assessment.api.model.v1.StagedStudentResultEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.StudentResult;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
public interface AssessmentStudentResultMapper {

    AssessmentStudentResultMapper mapper = Mappers.getMapper(AssessmentStudentResultMapper.class);

    @Mapping(target = "assessmentID", source = "assessmentEntity.assessmentID")
    StudentResult toStructure(StagedStudentResultEntity entity);

}
