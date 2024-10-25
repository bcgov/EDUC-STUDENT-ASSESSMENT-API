package ca.bc.gov.educ.eas.api.service.v1;

import ca.bc.gov.educ.eas.api.exception.SagaRuntimeException;
import ca.bc.gov.educ.eas.api.model.v1.EasSagaEntity;
import ca.bc.gov.educ.eas.api.service.v1.external.IDownstreamSyncService;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudent;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class StudentRegistrationOrchestrationService {

    protected final SagaService sagaService;

    @Getter(AccessLevel.PRIVATE)
    private final AssessmentStudentService assessmentStudentService;

    @Getter(AccessLevel.PRIVATE)
    private final IDownstreamSyncService downstreamSyncService;

    public StudentRegistrationOrchestrationService(AssessmentStudentService assessmentStudentService, IDownstreamSyncService downstreamSyncService, SagaService sagaService) {
        this.assessmentStudentService = assessmentStudentService;
        this.downstreamSyncService = downstreamSyncService;
        this.sagaService = sagaService;
    }

    @Retryable(maxAttempts = 5, retryFor = {Exception.class}, noRetryFor = {SagaRuntimeException.class}, backoff = @Backoff(multiplier = 2, delay = 2000))
    public void publishStudentRegistration(AssessmentStudent assessmentStudent, EasSagaEntity sagaEntity) {
        downstreamSyncService.publishRegistration(assessmentStudent);
    }

}
