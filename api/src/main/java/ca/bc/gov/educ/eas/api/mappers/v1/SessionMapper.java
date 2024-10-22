package ca.bc.gov.educ.eas.api.mappers.v1;

import ca.bc.gov.educ.eas.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.eas.api.mappers.UUIDMapper;
import ca.bc.gov.educ.eas.api.model.v1.SessionEntity;
import ca.bc.gov.educ.eas.api.struct.v1.Session;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class, AssessmentMapper.class})
public interface SessionMapper {

    SessionMapper mapper = Mappers.getMapper(SessionMapper.class);

    Session toStructure(SessionEntity entity);

    SessionEntity toEntity(Session session);

}
