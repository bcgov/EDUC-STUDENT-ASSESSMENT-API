package ca.bc.gov.educ.eas.api.batch.mappers;

import ca.bc.gov.educ.eas.api.batch.struct.AssessmentKeyDetail;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentKeyEntity;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentKeyFileEntity;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentKeyFileUpload;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static ca.bc.gov.educ.eas.api.properties.ApplicationProperties.EAS_API;

@Mapper
public interface AssessmentKeyFileMapper {
    AssessmentKeyFileMapper mapper = Mappers.getMapper(AssessmentKeyFileMapper.class);

    @Mapping(target = "updateUser", source = "upload.updateUser")
    @Mapping(target = "updateDate", expression = "java(java.time.LocalDateTime.now() )")
    @Mapping(target = "createUser", constant = EAS_API)
    @Mapping(target = "createDate",expression = "java(java.time.LocalDateTime.now() )")
    AssessmentKeyFileEntity toIncomingAssessmentKeyEntity(final AssessmentKeyFileUpload upload, final UUID assessmentID);

    @Mapping(target = "updateUser", constant = EAS_API)
    @Mapping(target = "updateDate", expression = "java(java.time.LocalDateTime.now() )")
    @Mapping(target = "createUser", constant = EAS_API)
    @Mapping(target = "createDate",expression = "java(java.time.LocalDateTime.now() )")
    AssessmentKeyEntity toAssessmentKeyEntity(AssessmentKeyDetail assessmentKeyDetail, AssessmentKeyFileEntity assessmentKeyFileEntity);

}
