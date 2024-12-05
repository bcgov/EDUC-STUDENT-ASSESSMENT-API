package ca.bc.gov.educ.eas.api.mappers.v1;

import ca.bc.gov.educ.eas.api.mappers.LocalDateTimeMapper;
import ca.bc.gov.educ.eas.api.mappers.UUIDMapper;
import ca.bc.gov.educ.eas.api.struct.v1.StudentMerge;
import ca.bc.gov.educ.eas.api.struct.v1.StudentMergeResult;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper(uses = {UUIDMapper.class, LocalDateTimeMapper.class, AssessmentMapper.class})
public interface StudentMergeMapper {

    StudentMergeMapper mapper = Mappers.getMapper(StudentMergeMapper.class);

    StudentMergeResult toStructure(StudentMerge studentMerge);
}
