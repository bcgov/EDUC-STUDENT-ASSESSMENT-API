package ca.bc.gov.educ.assessment.api.mappers.v1;

import ca.bc.gov.educ.assessment.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.assessment.api.mappers.UUIDMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentSession;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class, AssessmentMapper.class})
public interface SessionMapper {

    SessionMapper mapper = Mappers.getMapper(SessionMapper.class);

    AssessmentSession toStructure(AssessmentSessionEntity entity);

    AssessmentSessionEntity toEntity(AssessmentSession assessmentSession);

}
