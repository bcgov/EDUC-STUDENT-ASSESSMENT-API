package ca.bc.gov.educ.eas.api.mappers.v1;

import ca.bc.gov.educ.eas.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.eas.api.mappers.UUIDMapper;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.eas.api.struct.v1.Assessment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
public interface AssessmentMapper {

    AssessmentMapper mapper = Mappers.getMapper(AssessmentMapper.class);

    @Mapping(target = "sessionID", source = "sessionEntity.sessionID")
    @Mapping(target = "students", ignore = true)
    Assessment toStructure(AssessmentEntity entity);

    @Mapping(target = "sessionEntity.sessionID", source = "session.sessionID")
    AssessmentEntity toEntity(Assessment session);

}
