package ca.bc.gov.educ.eas.api.service.v1;

import ca.bc.gov.educ.eas.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.eas.api.model.v1.AssessmentEntity;
import ca.bc.gov.educ.eas.api.model.v1.SessionEntity;
import ca.bc.gov.educ.eas.api.repository.v1.AssessmentRepository;
import ca.bc.gov.educ.eas.api.repository.v1.SessionRepository;
import ca.bc.gov.educ.eas.api.util.TransformUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final SessionRepository sessionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AssessmentEntity updateAssessment(AssessmentEntity assessmentEntity){
        AssessmentEntity currentAssessmentEntity = assessmentRepository.findById(assessmentEntity.getAssessmentID()).orElseThrow(() ->
                new EntityNotFoundException(AssessmentEntity.class, "Assessment", assessmentEntity.getAssessmentID().toString())
        );
        BeanUtils.copyProperties(assessmentEntity, currentAssessmentEntity, "assessmentEntity", "createUser", "createDate", "assessmentStudentStatusCode");
        TransformUtil.uppercaseFields(currentAssessmentEntity);
        return assessmentRepository.save(assessmentEntity);

    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteAssessment(UUID assessmentID) {
        var assessmentOptional = assessmentRepository.findById(assessmentID);

        if (assessmentOptional.isPresent()) {
            AssessmentEntity assessment = assessmentOptional.get();
            SessionEntity session = assessment.getSessionEntity();
            session.getAssessments().remove(assessment);
            sessionRepository.save(session);
            assessmentRepository.delete(assessment);
        } else {
            log.error("Assessment not found with id: {}", assessmentID);
        }

    }

}