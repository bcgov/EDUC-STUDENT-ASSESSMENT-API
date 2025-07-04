package ca.bc.gov.educ.assessment.api.service.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEventEntity;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentEventRepository;
import ca.bc.gov.educ.assessment.api.service.v1.external.IDownstreamSyncService;
import com.nimbusds.jose.util.Pair;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class SessionApprovalOrchestrationService {

    protected final SagaService sagaService;

    @Getter(AccessLevel.PRIVATE)
    private final AssessmentStudentService assessmentStudentService;

    @Getter(AccessLevel.PRIVATE)
    private final AssessmentEventRepository assessmentEventRepository;

    @Getter(AccessLevel.PRIVATE)
    private final IDownstreamSyncService downstreamSyncService;

    public SessionApprovalOrchestrationService(AssessmentStudentService assessmentStudentService, IDownstreamSyncService downstreamSyncService, SagaService sagaService, AssessmentEventRepository assessmentEventRepository) {
        this.assessmentStudentService = assessmentStudentService;
        this.downstreamSyncService = downstreamSyncService;
        this.sagaService = sagaService;
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

}
