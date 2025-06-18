package ca.bc.gov.educ.assessment.api.mappers.v1;

import ca.bc.gov.educ.assessment.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.assessment.api.mappers.UUIDMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentListItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
public interface AssessmentStudentListItemMapper {

    AssessmentStudentListItemMapper mapper = Mappers.getMapper(AssessmentStudentListItemMapper.class);

    @Mapping(target = "assessmentID", source = "assessmentEntity.assessmentID")
    @Mapping(target = "sessionID", source = "assessmentEntity.assessmentSessionEntity.sessionID")
    @Mapping(target = "assessmentTypeCode", source = "assessmentEntity.assessmentTypeCode")
    @Mapping(target = "courseMonth", source = "assessmentEntity.assessmentSessionEntity.courseMonth")
    @Mapping(target = "courseYear", source = "assessmentEntity.assessmentSessionEntity.courseYear")
    @Mapping(target = "assessmentCenterSchoolID", source = "assessmentCenterSchoolID")
    @Mapping(target = "surname", source = "surname")
    @Mapping(target = "givenName", source = "givenName")
    AssessmentStudentListItem toStructure(AssessmentStudentEntity entity);

}
