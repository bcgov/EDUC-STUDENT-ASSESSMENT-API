package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEventEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentEventRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import com.nimbusds.jose.util.Pair;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class SessionApprovalOrchestrationService {

    protected final SagaService sagaService;

    @Getter(AccessLevel.PRIVATE)
    private final AssessmentStudentService assessmentStudentService;

    @Getter(AccessLevel.PRIVATE)
    private final AssessmentSessionRepository assessmentSessionRepository;

    @Getter(AccessLevel.PRIVATE)
    private final AssessmentEventRepository assessmentEventRepository;

    public SessionApprovalOrchestrationService(AssessmentStudentService assessmentStudentService, SagaService sagaService, AssessmentSessionRepository assessmentSessionRepository, AssessmentEventRepository assessmentEventRepository) {
        this.assessmentStudentService = assessmentStudentService;
        this.sagaService = sagaService;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.assessmentEventRepository = assessmentEventRepository;
    }
    
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Pair<List<AssessmentEventEntity>, List<UUID>> getStudentRegistrationEvents(UUID sessionID) {
        var studentIDs = assessmentStudentService.getAllStudentIDsInSessionFromResultsStaging(sessionID);
        var events = studentIDs.stream()
                .map(studentID -> assessmentStudentService.generateStudentUpdatedEvent(studentID.toString()))
                .toList();
        assessmentEventRepository.saveAll(events);
        return Pair.of(events, studentIDs);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AssessmentSessionEntity updateSessionCompletionDate(UUID sessionID) {
        var assessmentSessionEntity = assessmentSessionRepository.findById(sessionID).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class));
        assessmentSessionEntity.setCompletionDate(LocalDateTime.now());
        return assessmentSessionRepository.save(assessmentSessionEntity);
    }
}
