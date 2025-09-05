package ca.bc.gov.educ.assessment.api.mappers.v1;

import ca.bc.gov.educ.assessment.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.assessment.api.mappers.UUIDMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentFormEntity;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentForm;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class, AssessmentComponentMapper.class})
public interface AssessmentFormMapper {

    AssessmentFormMapper mapper = Mappers.getMapper(AssessmentFormMapper.class);

    @Mapping(target = "assessmentID", source = "assessmentEntity.assessmentID")
    AssessmentForm toStructure(AssessmentFormEntity entity);

}
