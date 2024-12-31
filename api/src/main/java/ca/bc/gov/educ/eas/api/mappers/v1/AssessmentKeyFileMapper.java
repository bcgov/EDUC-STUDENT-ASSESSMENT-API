package ca.bc.gov.educ.eas.api.mappers.v1;

import ca.bc.gov.educ.eas.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.eas.api.mappers.UUIDMapper;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentKeyFileEntity;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentKeyFile;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
public interface AssessmentKeyFileMapper {

    AssessmentKeyFileMapper mapper = Mappers.getMapper(AssessmentKeyFileMapper.class);

    AssessmentKeyFile toStructure(final AssessmentKeyFileEntity assessmentKeyFileEntity);
}
