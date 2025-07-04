package ca.bc.gov.educ.assessment.api.orchestrator;

import ca.bc.gov.educ.assessment.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.assessment.api.messaging.MessagePublisher;
import ca.bc.gov.educ.assessment.api.messaging.jetstream.Publisher;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSagaEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSagaEventStatesEntity;
import ca.bc.gov.educ.assessment.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.assessment.api.service.v1.SagaService;
import ca.bc.gov.educ.assessment.api.service.v1.SessionApprovalOrchestrationService;
import ca.bc.gov.educ.assessment.api.service.v1.XAMFileService;
import ca.bc.gov.educ.assessment.api.struct.Event;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static ca.bc.gov.educ.assessment.api.constants.EventOutcome.GRAD_STUDENT_API_NOTIFIED;
import static ca.bc.gov.educ.assessment.api.constants.EventOutcome.XAM_FILES_GENERATED_AND_UPLOADED;
import static ca.bc.gov.educ.assessment.api.constants.EventType.GENERATE_XAM_FILES_AND_UPLOAD;
import static ca.bc.gov.educ.assessment.api.constants.EventType.NOTIFY_GRAD_OF_UPDATED_STUDENTS;
import static ca.bc.gov.educ.assessment.api.constants.SagaEnum.GENERATE_XAM_FILES;
import static ca.bc.gov.educ.assessment.api.constants.TopicsEnum.XAM_FILE_GENERATION_TOPIC;

@Slf4j
@Component
public class SessionApprovalOrchestrator extends BaseOrchestrator<String> {

    private final Publisher publisher;
    private final XAMFileService xamFileService;
    private final SessionApprovalOrchestrationService sessionApprovalOrchestrationService;
    private final SagaService sagaService;

    protected SessionApprovalOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, Publisher publisher, final XAMFileService xamFileService, SessionApprovalOrchestrationService sessionApprovalOrchestrationService) {
        super(sagaService, messagePublisher, String.class, GENERATE_XAM_FILES.toString(), XAM_FILE_GENERATION_TOPIC.toString());
        this.publisher = publisher;
        this.xamFileService = xamFileService;
        this.sagaService = sagaService;
        this.sessionApprovalOrchestrationService = sessionApprovalOrchestrationService;
    }

    @Override
    public void populateStepsToExecuteMap() {
        this.stepBuilder()
            .begin(GENERATE_XAM_FILES_AND_UPLOAD, this::generateXAMFilesAndUpload)
            .step(GENERATE_XAM_FILES_AND_UPLOAD, XAM_FILES_GENERATED_AND_UPLOADED, NOTIFY_GRAD_OF_UPDATED_STUDENTS, this::notifyGradOfUpdatedStudents)
            .end(NOTIFY_GRAD_OF_UPDATED_STUDENTS, GRAD_STUDENT_API_NOTIFIED);
    }

    private void generateXAMFilesAndUpload(Event event, AssessmentSagaEntity saga, String payload) throws JsonProcessingException {
        final AssessmentSagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(GENERATE_XAM_FILES_AND_UPLOAD.toString());
        saga.setStatus(SagaStatusEnum.IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        UUID sessionID = UUID.fromString(payload);
        xamFileService.generateAndUploadXamFiles(sessionID);

        final Event nextEvent = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(GENERATE_XAM_FILES_AND_UPLOAD)
                .eventOutcome(XAM_FILES_GENERATED_AND_UPLOADED)
                .eventPayload(JsonUtil.getJsonStringFromObject("All schools processed"))
                .build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        log.debug("Posted completion message for saga step generateXAMFilesAndUpload: {}", saga.getSagaId());
    }

    private void notifyGradOfUpdatedStudents(Event event, AssessmentSagaEntity saga, String payload) throws JsonProcessingException {
        final AssessmentSagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(NOTIFY_GRAD_OF_UPDATED_STUDENTS.toString());
        saga.setStatus(SagaStatusEnum.IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
        
        UUID sessionID = UUID.fromString(payload);
        var pair = sessionApprovalOrchestrationService.getStudentRegistrationEvents(sessionID);
        pair.getLeft().forEach(publisher::dispatchChoreographyEvent);
        final Event nextEvent = Event.builder().sagaId(saga.getSagaId())
                .eventType(NOTIFY_GRAD_OF_UPDATED_STUDENTS).eventOutcome(GRAD_STUDENT_API_NOTIFIED)
                .eventPayload(JsonUtil.getJsonStringFromObject("GRAD system notified of " + pair.getRight().size() + " updated students"))
                .build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        log.debug("Posted completion message for saga step notifyGradOfUpdatedStudents: {}", saga.getSagaId());
    }

    public void startXamFileGenerationSaga(UUID sessionID) throws JsonProcessingException {
        String payload = sessionID.toString();
        AssessmentSagaEntity saga = sagaService.createSagaRecordInDB(
                this.getSagaName(),
                "system",
                payload,
                sessionID
        );
        this.startSaga(saga);
    }
}
