package ca.bc.gov.educ.assessment.api.orchestrator;

import ca.bc.gov.educ.assessment.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.messaging.MessagePublisher;
import ca.bc.gov.educ.assessment.api.messaging.jetstream.Publisher;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSagaEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSagaEventStatesEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.service.v1.*;
import ca.bc.gov.educ.assessment.api.struct.Event;
import ca.bc.gov.educ.assessment.api.struct.v1.ApprovalSagaData;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static ca.bc.gov.educ.assessment.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.assessment.api.constants.EventType.*;
import static ca.bc.gov.educ.assessment.api.constants.SagaEnum.SESSION_APPROVAL;
import static ca.bc.gov.educ.assessment.api.constants.TopicsEnum.XAM_FILE_GENERATION_TOPIC;
import static ca.bc.gov.educ.assessment.api.properties.ApplicationProperties.STUDENT_ASSESSMENT_API;

@Slf4j
@Component
public class SessionApprovalOrchestrator extends BaseOrchestrator<ApprovalSagaData> {

    private final XAMFileService xamFileService;
    private final SessionApprovalOrchestrationService sessionApprovalOrchestrationService;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final SagaService sagaService;
    private final EmailService emailService;
    private final AssessmentStudentService assessmentStudentService;
    private final Publisher publisher;

    protected SessionApprovalOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, final XAMFileService xamFileService, SessionApprovalOrchestrationService sessionApprovalOrchestrationService, AssessmentSessionRepository assessmentSessionRepository, EmailService emailService, AssessmentStudentService assessmentStudentService, Publisher publisher) {
        super(sagaService, messagePublisher, ApprovalSagaData.class, SESSION_APPROVAL.toString(), XAM_FILE_GENERATION_TOPIC.toString());
        this.xamFileService = xamFileService;
        this.sagaService = sagaService;
        this.sessionApprovalOrchestrationService = sessionApprovalOrchestrationService;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.emailService = emailService;
        this.assessmentStudentService = assessmentStudentService;
        this.publisher = publisher;
    }

    @Override
    public void populateStepsToExecuteMap() {
        this.stepBuilder()
            .begin(GENERATE_XAM_FILES_AND_UPLOAD, this::generateXAMFilesAndUpload)
            .step(GENERATE_XAM_FILES_AND_UPLOAD, XAM_FILES_GENERATED_AND_UPLOADED, NOTIFY_MYED_OF_UPDATED_STUDENTS, this::notifyMyEDOfApproval)
            .step(NOTIFY_MYED_OF_UPDATED_STUDENTS, MYED_NOTIFIED, MARK_SESSION_COMPLETION_DATE, this::markSessionCompletionDate)
            .step(MARK_SESSION_COMPLETION_DATE, COMPLETION_DATE_SET, FLIP_GRAD_FLAGS_FOR_SESSION_STUDENTS, this::flipFlagsForAllStudentsInSession)
            .step(FLIP_GRAD_FLAGS_FOR_SESSION_STUDENTS, GRAD_FLAGS_FLIPPED_FOR_STUDENTS, MARK_STAGED_STUDENTS_READY_FOR_TRANSFER, this::markStagedStudentsReadyForTransfer)
            .step(MARK_STAGED_STUDENTS_READY_FOR_TRANSFER, STAGED_STUDENTS_MARKED_READY_FOR_TRANSFER, DELETE_PEN_ISSUE_STUDENTS_FROM_STAGING, this::deleteStudentsWithPenIssuesFromStaging)
            .end(DELETE_PEN_ISSUE_STUDENTS_FROM_STAGING, DELETE_PEN_ISSUE_STUDENTS_FROM_STAGING_COMPLETED);
    }

    private void markSessionCompletionDate(Event event, AssessmentSagaEntity saga, ApprovalSagaData approvalSagaData) throws JsonProcessingException {
        final AssessmentSagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(MARK_SESSION_COMPLETION_DATE.toString());
        saga.setStatus(SagaStatusEnum.IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        UUID sessionID = UUID.fromString(approvalSagaData.getSessionID());
        sessionApprovalOrchestrationService.updateSessionCompletionDate(sessionID);

        final Event nextEvent = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(MARK_SESSION_COMPLETION_DATE)
                .eventOutcome(COMPLETION_DATE_SET)
                .eventPayload(JsonUtil.getJsonStringFromObject(approvalSagaData))
                .build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        log.debug("Posted completion message for saga step markSessionCompletionDate: {}", saga.getSagaId());
    }

    private void flipFlagsForAllStudentsInSession(Event event, AssessmentSagaEntity saga, ApprovalSagaData approvalSagaData) throws JsonProcessingException {
        final AssessmentSagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(FLIP_GRAD_FLAGS_FOR_SESSION_STUDENTS.toString());
        saga.setStatus(SagaStatusEnum.IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        UUID sessionID = UUID.fromString(approvalSagaData.getSessionID());
        assessmentSessionRepository.findById(sessionID).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class));
        assessmentStudentService.flipFlagsForAllStudentsInSession(sessionID);
        
        final Event nextEvent = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(FLIP_GRAD_FLAGS_FOR_SESSION_STUDENTS)
                .eventOutcome(GRAD_FLAGS_FLIPPED_FOR_STUDENTS)
                .eventPayload(JsonUtil.getJsonStringFromObject(approvalSagaData))
                .build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        log.debug("Posted completion message for saga step flipFlagsForAllStudentsInSession: {}", saga.getSagaId());
    }

    private void markStagedStudentsReadyForTransfer(Event event, AssessmentSagaEntity saga, ApprovalSagaData approvalSagaData) throws JsonProcessingException {
        final AssessmentSagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(MARK_STAGED_STUDENTS_READY_FOR_TRANSFER.toString());
        saga.setStatus(SagaStatusEnum.IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        assessmentStudentService.markStagedStudentsReadyForTransferOrDelete();

        final Event nextEvent = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(MARK_STAGED_STUDENTS_READY_FOR_TRANSFER)
                .eventOutcome(STAGED_STUDENTS_MARKED_READY_FOR_TRANSFER)
                .eventPayload(JsonUtil.getJsonStringFromObject(approvalSagaData))
                .build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        log.debug("Posted completion message for saga step markStagedStudentsReadyForTransfer: {}", saga.getSagaId());
    }

    private void deleteStudentsWithPenIssuesFromStaging(Event event, AssessmentSagaEntity saga, ApprovalSagaData approvalSagaData) throws JsonProcessingException {
        final AssessmentSagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(DELETE_PEN_ISSUE_STUDENTS_FROM_STAGING.toString());
        saga.setStatus(SagaStatusEnum.IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        assessmentStudentService.deleteStudentsWithPenIssuesFromStaging();

        final Event nextEvent = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(DELETE_PEN_ISSUE_STUDENTS_FROM_STAGING)
                .eventOutcome(DELETE_PEN_ISSUE_STUDENTS_FROM_STAGING_COMPLETED)
                .eventPayload(JsonUtil.getJsonStringFromObject(approvalSagaData))
                .build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
    }

    private void generateXAMFilesAndUpload(Event event, AssessmentSagaEntity saga, ApprovalSagaData approvalSagaData) throws JsonProcessingException {
        final AssessmentSagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(GENERATE_XAM_FILES_AND_UPLOAD.toString());
        saga.setStatus(SagaStatusEnum.IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        UUID sessionID = UUID.fromString(approvalSagaData.getSessionID());
        var assessmentSessionEntity = assessmentSessionRepository.findById(sessionID).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class));
        xamFileService.generateAndUploadXamFiles(assessmentSessionEntity);

        final Event nextEvent = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(GENERATE_XAM_FILES_AND_UPLOAD)
                .eventOutcome(XAM_FILES_GENERATED_AND_UPLOADED)
                .eventPayload(JsonUtil.getJsonStringFromObject(approvalSagaData))
                .build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        log.debug("Posted completion message for saga step generateXAMFilesAndUpload: {}", saga.getSagaId());
    }

    private void notifyMyEDOfApproval(Event event, AssessmentSagaEntity saga, ApprovalSagaData approvalSagaData) throws JsonProcessingException {
        final AssessmentSagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(NOTIFY_MYED_OF_UPDATED_STUDENTS.toString());
        saga.setStatus(SagaStatusEnum.IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        emailService.sendMyEDApprovalEmail(approvalSagaData);
        
        final Event nextEvent = Event.builder().sagaId(saga.getSagaId())
                .eventType(NOTIFY_MYED_OF_UPDATED_STUDENTS).eventOutcome(MYED_NOTIFIED)
                .eventPayload(JsonUtil.getJsonStringFromObject(approvalSagaData))
                .build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        log.debug("Posted completion message for saga step notifyMyEDOfApproval: {}", saga.getSagaId());
    }

    @Async("subscriberExecutor")
    public void startXamFileGenerationSaga(UUID assessmentSessionID) throws JsonProcessingException {
        var approvalSaga = ApprovalSagaData.builder().sessionID(assessmentSessionID.toString()).build();
        String payload = JsonUtil.getJsonStringFromObject(approvalSaga);
        AssessmentSagaEntity saga = sagaService.createSagaRecordInDB(
                this.getSagaName(),
                STUDENT_ASSESSMENT_API,
                payload,
                null,
                null,
                null,
                null,
                assessmentSessionID,
                null
        );
        this.startSaga(saga);
    }
}
