package ca.bc.gov.educ.eas.api.mappers;

import ca.bc.gov.educ.eas.api.model.v1.SessionEntity;
import ca.bc.gov.educ.eas.api.struct.v1.Session;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.factory.Mappers;

/**
 * Mapper for converting between SessionEntity and Session DTO.
 */
@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
public interface SessionMapper {

    SessionMapper mapper = Mappers.getMapper(SessionMapper.class);

    /**
     * Conversion from Session Entity to DTO.
     * @param entity SessionEntity
     * @return Session
     */
    @Mapping(source = "courseSession", target = "courseSession")
    @Mapping(source = "statusCode", target = "status")
    Session toStructure(SessionEntity entity);

    /**
     * Conversion from Session DTO to Entity.
     * @param session Session
     * @return SessionEntity
     */
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    SessionEntity toEntity(Session session);

}
