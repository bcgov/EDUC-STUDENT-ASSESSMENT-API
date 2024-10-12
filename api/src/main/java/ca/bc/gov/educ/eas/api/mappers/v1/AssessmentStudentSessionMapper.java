package ca.bc.gov.educ.eas.api.mappers.v1;

import ca.bc.gov.educ.eas.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentStudentEntity;
import ca.bc.gov.educ.eas.api.repository.v1.AssessmentRepository;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudent;
import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class AssessmentStudentSessionMapper implements AssessmentStudentMapper {

    private final AssessmentRepository assessmentRepository;

    @Autowired
    public AssessmentStudentSessionMapper(AssessmentRepository assessmentRepository) {
        this.assessmentRepository = assessmentRepository;
    }

    @Override
    public AssessmentStudent toStructure(AssessmentStudentEntity entity) {
        return mapper.toStructure(entity);
    }

    @Override
    public AssessmentStudent mapAssessment(AssessmentStudent assessmentStudent) {
        Optional<AssessmentEntity> assessmentEntity;
        if (StringUtils.isNotEmpty(assessmentStudent.getAssessmentID())) {
            assessmentEntity = assessmentRepository.findById(UUID.fromString(assessmentStudent.getAssessmentID()));
        } else {
            assessmentEntity = assessmentRepository.findBySessionIdAndAssessmentType(UUID.fromString(assessmentStudent.getSessionID()), assessmentStudent.getAssessmentTypeCode());
        }
        if (assessmentEntity.isPresent()) {
            assessmentStudent.setAssessmentID(assessmentEntity.get().getAssessmentID().toString());
        } else {
            assessmentStudent.setAssessmentID(null);
        }
        return assessmentStudent;
    }

    @Override
    public AssessmentStudentEntity toModel(AssessmentStudent assessmentStudent) {
        return mapper.toModel(assessmentStudent);
    }

}
