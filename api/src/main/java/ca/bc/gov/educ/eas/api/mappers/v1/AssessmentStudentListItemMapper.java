package ca.bc.gov.educ.eas.api.mappers.v1;

import ca.bc.gov.educ.eas.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.eas.api.mappers.UUIDMapper;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudentListItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
public interface AssessmentStudentListItemMapper {

    AssessmentStudentListItemMapper mapper = Mappers.getMapper(AssessmentStudentListItemMapper.class);

    @Mapping(target = "assessmentID", source = "assessmentEntity.assessmentID")
    @Mapping(target = "sessionID", source = "assessmentEntity.sessionEntity.sessionID")
    @Mapping(target = "assessmentTypeCode", source = "assessmentEntity.assessmentTypeCode")
    @Mapping(target = "courseMonth", source = "assessmentEntity.sessionEntity.courseMonth")
    @Mapping(target = "courseYear", source = "assessmentEntity.sessionEntity.courseYear")
    @Mapping(target = "assessmentCenterID", source = "assessmentCenterID")
    @Mapping(target = "surName", source = "surName")
    AssessmentStudentListItem toStructure(AssessmentStudentEntity entity);

}
