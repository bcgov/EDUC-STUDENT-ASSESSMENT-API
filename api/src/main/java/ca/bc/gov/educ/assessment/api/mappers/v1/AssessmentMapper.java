package ca.bc.gov.educ.assessment.api.mappers.v1;

import ca.bc.gov.educ.assessment.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.assessment.api.mappers.UUIDMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.Assessment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
public interface AssessmentMapper {

    AssessmentMapper mapper = Mappers.getMapper(AssessmentMapper.class);

    @Mapping(target = "sessionID", source = "sessionEntity.sessionID")
    Assessment toStructure(AssessmentEntity entity);

    @Mapping(target = "sessionEntity.sessionID", source = "session.sessionID")
    AssessmentEntity toEntity(Assessment session);

}
