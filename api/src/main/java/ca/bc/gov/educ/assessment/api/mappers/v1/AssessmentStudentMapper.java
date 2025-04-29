package ca.bc.gov.educ.assessment.api.mappers.v1;

import ca.bc.gov.educ.assessment.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.assessment.api.mappers.UUIDMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.assessment.api.util.TransformUtil;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
public interface AssessmentStudentMapper {

    AssessmentStudentMapper mapper = Mappers.getMapper(AssessmentStudentMapper.class);

    @Mapping(target = "assessmentID", source = "assessmentEntity.assessmentID")
    AssessmentStudent toStructure(AssessmentStudentEntity entity);

    @Mapping(target = "assessmentEntity.assessmentID", source = "assessmentID")
    @Mapping(target = "assessmentCenterID", source = "assessmentCenterID")
    @Mapping(target = "givenName", source = "givenName")
    AssessmentStudentEntity toModel(AssessmentStudent assessmentStudent);

    @AfterMapping
    default void transformToUpperCase(AssessmentStudentEntity assessmentStudentEntity, @MappingTarget AssessmentStudent.AssessmentStudentBuilder assessmentStudent) {
        TransformUtil.uppercaseFields(assessmentStudentEntity);
    }
}
