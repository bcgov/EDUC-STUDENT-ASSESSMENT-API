package ca.bc.gov.educ.assessment.api.mappers.v1;

import ca.bc.gov.educ.assessment.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.assessment.api.mappers.UUIDMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudentShowItem;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class, AssessmentStudentComponentMapper.class})
@DecoratedWith(AssessmentStudentShowItemMapperDecorator.class)
public interface AssessmentStudentShowItemMapper {
    AssessmentStudentShowItemMapper mapper = Mappers.getMapper(AssessmentStudentShowItemMapper.class);

    @Mapping(target = "assessmentID", source = "assessmentEntity.assessmentID")
    @Mapping(target = "sessionID", source = "assessmentEntity.assessmentSessionEntity.sessionID")
    @Mapping(target = "assessmentTypeCode", source = "assessmentEntity.assessmentTypeCode")
    @Mapping(target = "courseMonth", source = "assessmentEntity.assessmentSessionEntity.courseMonth")
    @Mapping(target = "courseYear", source = "assessmentEntity.assessmentSessionEntity.courseYear")
    @Mapping(target = "assessmentCenterSchoolID", source = "assessmentCenterSchoolID")
    @Mapping(target = "surname", source = "surname")
    @Mapping(target = "givenName", source = "givenName")
    @Mapping(target = "rawScore", source = "rawScore")
    @Mapping(target = "mcTotal", source = "mcTotal")
    @Mapping(target = "oeTotal", source = "oeTotal")
    @Mapping(target = "assessmentStudentComponents", source = "assessmentStudentComponentEntities")
    AssessmentStudentShowItem toStructure(AssessmentStudentEntity entity);
}
