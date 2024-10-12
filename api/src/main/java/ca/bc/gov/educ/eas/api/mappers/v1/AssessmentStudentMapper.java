package ca.bc.gov.educ.eas.api.mappers.v1;

import ca.bc.gov.educ.eas.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.eas.api.mappers.UUIDMapper;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.eas.api.util.TransformUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
public interface AssessmentStudentMapper {

    AssessmentStudentMapper mapper = Mappers.getMapper(AssessmentStudentMapper.class);

    @Mapping(target = "assessmentID", source = "assessmentEntity.assessmentID")
    @Mapping(target = "sessionID", source = "assessmentEntity.sessionEntity.sessionID")
    @Mapping(target = "assessmentTypeCode", source = "assessmentEntity.assessmentTypeCode")
    AssessmentStudent toStructure(AssessmentStudentEntity entity);

    AssessmentStudent mapAssessment(AssessmentStudent assessmentStudent);

    @Mapping(target = "assessmentEntity.assessmentID", source = "assessmentID")
    @Mapping(target = "assessmentEntity.sessionEntity.sessionID", source = "sessionID")
    @Mapping(target = "assessmentEntity.assessmentTypeCode", source = "assessmentTypeCode")
    AssessmentStudentEntity toModel(AssessmentStudent assessmentStudent);

    @AfterMapping
    default void transformToUpperCase(AssessmentStudentEntity assessmentStudentEntity, @MappingTarget AssessmentStudent.AssessmentStudentBuilder assessmentStudent) {
        TransformUtil.uppercaseFields(assessmentStudentEntity);
    }
}
