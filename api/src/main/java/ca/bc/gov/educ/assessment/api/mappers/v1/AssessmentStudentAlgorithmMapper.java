package ca.bc.gov.educ.assessment.api.mappers.v1;

import ca.bc.gov.educ.assessment.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.assessment.api.mappers.UUIDMapper;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.assessment.api.struct.external.gradalgorithmapi.AssessmentStudentForAlgorithm;
import org.mapstruct.DecoratedWith;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class})
@DecoratedWith(AssessmentStudentAlgorithmMapperDecorator.class)
public interface AssessmentStudentAlgorithmMapper {

    AssessmentStudentAlgorithmMapper mapper = Mappers.getMapper(AssessmentStudentAlgorithmMapper.class);

    @Mapping(target = "assessmentCode", source = "assessmentEntity.assessmentTypeCode")
    @Mapping(target = "specialCase", source = "provincialSpecialCaseCode")
    AssessmentStudentForAlgorithm toStructure(AssessmentStudentEntity entity);
}
