package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentRepository;
import ca.bc.gov.educ.assessment.api.util.TransformUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;

    public AssessmentEntity getAssessment(UUID assessmentID){
        Optional<AssessmentEntity> assessmentOptionalEntity = assessmentRepository.findById(assessmentID);
        return assessmentOptionalEntity.orElseThrow(() -> new EntityNotFoundException(AssessmentEntity.class, "assessmentID", assessmentID.toString()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AssessmentEntity createAssessment(AssessmentEntity assessmentEntity){
        TransformUtil.uppercaseFields(assessmentEntity);
        return assessmentRepository.save(assessmentEntity);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AssessmentEntity updateAssessment(AssessmentEntity assessmentEntity){
        AssessmentEntity currentAssessmentEntity = assessmentRepository.findById(assessmentEntity.getAssessmentID()).orElseThrow(() ->
                new EntityNotFoundException(AssessmentEntity.class, "Assessment", assessmentEntity.getAssessmentID().toString())
        );
        BeanUtils.copyProperties(assessmentEntity, currentAssessmentEntity, "assessmentForms","assessmentEntity", "createUser", "createDate");
        TransformUtil.uppercaseFields(currentAssessmentEntity);
        return assessmentRepository.save(assessmentEntity);

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteAssessment(UUID assessmentID) {
        Optional<AssessmentEntity> assessmentOptionalEntity = assessmentRepository.findById(assessmentID);
        AssessmentEntity assessmentEntity = assessmentOptionalEntity.orElseThrow(() -> new EntityNotFoundException(AssessmentEntity.class, "assessmentID", assessmentID.toString()));
        assessmentRepository.delete(assessmentEntity);
    }

}
