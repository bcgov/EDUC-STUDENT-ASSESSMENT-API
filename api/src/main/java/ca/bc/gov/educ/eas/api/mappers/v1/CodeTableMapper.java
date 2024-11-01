package ca.bc.gov.educ.eas.api.mappers.v1;

import ca.bc.gov.educ.eas.api.model.v1.AssessmentTypeCodeEntity;
import ca.bc.gov.educ.eas.api.model.v1.ProvincialSpecialCaseCodeEntity;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentTypeCode;
import ca.bc.gov.educ.eas.api.struct.v1.ProvincialSpecialCaseCode;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface CodeTableMapper {
    CodeTableMapper mapper = Mappers.getMapper(CodeTableMapper.class);

    AssessmentTypeCode toStructure(AssessmentTypeCodeEntity entity);

    ProvincialSpecialCaseCode toStructure(ProvincialSpecialCaseCodeEntity entity);
}
