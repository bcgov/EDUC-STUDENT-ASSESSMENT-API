package ca.bc.gov.educ.assessment.api.mappers.v1;

import ca.bc.gov.educ.assessment.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.assessment.api.mappers.UUIDMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentDetails;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class, AssessmentFormMapper.class})
public interface AssessmentDetailsMapper {
    AssessmentDetailsMapper mapper = Mappers.getMapper(AssessmentDetailsMapper.class);

    @Mapping(target = "sessionID", source = "assessmentSessionEntity.sessionID")
    @Mapping(target = "courseMonth", source = "assessmentSessionEntity.courseMonth")
    @Mapping(target = "courseYear", source = "assessmentSessionEntity.courseYear")
    @Mapping(target = "schoolYear", source = "assessmentSessionEntity.schoolYear")
    @Mapping(target = "assessmentForms", source = "assessmentForms")
    AssessmentDetails toStructure(AssessmentEntity entity);
}
