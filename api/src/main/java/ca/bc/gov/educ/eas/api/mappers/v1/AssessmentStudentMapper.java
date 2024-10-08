package ca.bc.gov.educ.eas.api.mappers.v1;

import ca.bc.gov.educ.eas.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.eas.api.mappers.UUIDMapper;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
public interface AssessmentStudentMapper {

  AssessmentStudentMapper mapper = Mappers.getMapper(AssessmentStudentMapper.class);

  @Mapping(target = "sessionID", source = "sessionEntity.sessionID")
  AssessmentStudent toStructure(AssessmentStudentEntity entity);

  @Mapping(target = "sessionEntity.sessionID", source = "sessionID")
  AssessmentStudentEntity toModel(AssessmentStudent assessmentStudent);
}
