package ca.bc.gov.educ.assessment.api.orchestrator;

import ca.bc.gov.educ.assessment.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.assessment.api.exception.EntityNotFoundException;
import ca.bc.gov.educ.assessment.api.messaging.MessagePublisher;
import ca.bc.gov.educ.assessment.api.messaging.jetstream.Publisher;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSagaEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSagaEventStatesEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSessionEntity;
import ca.bc.gov.educ.assessment.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.assessment.api.properties.EmailProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.AssessmentSessionRepository;
import ca.bc.gov.educ.assessment.api.service.v1.*;
import ca.bc.gov.educ.assessment.api.struct.Event;
import ca.bc.gov.educ.assessment.api.struct.v1.ApprovalSagaData;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

import static ca.bc.gov.educ.assessment.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.assessment.api.constants.EventType.*;
import static ca.bc.gov.educ.assessment.api.constants.SagaEnum.GENERATE_XAM_FILES;
import static ca.bc.gov.educ.assessment.api.constants.TopicsEnum.XAM_FILE_GENERATION_TOPIC;

@Slf4j
@Component
public class SessionApprovalOrchestrator extends BaseOrchestrator<ApprovalSagaData> {

    private final Publisher publisher;
    private final XAMFileService xamFileService;
    private final SessionApprovalOrchestrationService sessionApprovalOrchestrationService;
    private final AssessmentSessionRepository assessmentSessionRepository;
    private final SagaService sagaService;
    private final EmailProperties emailProperties;
    private final EmailService emailService;
    private final AssessmentStudentService assessmentStudentService;

    protected SessionApprovalOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, Publisher publisher, final XAMFileService xamFileService, SessionApprovalOrchestrationService sessionApprovalOrchestrationService, AssessmentSessionRepository assessmentSessionRepository, EmailProperties emailProperties, EmailService emailService, AssessmentStudentService assessmentStudentService) {
        super(sagaService, messagePublisher, ApprovalSagaData.class, GENERATE_XAM_FILES.toString(), XAM_FILE_GENERATION_TOPIC.toString());
        this.publisher = publisher;
        this.xamFileService = xamFileService;
        this.sagaService = sagaService;
        this.sessionApprovalOrchestrationService = sessionApprovalOrchestrationService;
        this.assessmentSessionRepository = assessmentSessionRepository;
        this.emailProperties = emailProperties;
        this.emailService = emailService;
        this.assessmentStudentService = assessmentStudentService;
    }

    @Override
    public void populateStepsToExecuteMap() {
        this.stepBuilder()
            .begin(MARK_STAGED_STUDENTS_READY_FOR_TRANSFER, this::markStagedStudentsReadyForTransfer)// todo transfer last
            .step(MARK_STAGED_STUDENTS_READY_FOR_TRANSFER, STAGED_STUDENTS_MARKED_READY_FOR_TRANSFER, GENERATE_XAM_FILES_AND_UPLOAD, this::generateXAMFilesAndUpload) // todo point at staging
            .step(GENERATE_XAM_FILES_AND_UPLOAD, XAM_FILES_GENERATED_AND_UPLOADED, NOTIFY_GRAD_OF_UPDATED_STUDENTS, this::notifyGradOfUpdatedStudents)
            .step(NOTIFY_GRAD_OF_UPDATED_STUDENTS, GRAD_STUDENT_API_NOTIFIED, NOTIFY_MYED_OF_UPDATED_STUDENTS, this::notifyMyEDOfApproval)
            .step(NOTIFY_MYED_OF_UPDATED_STUDENTS, MYED_NOTIFIED, MARK_SESSION_COMPLETION_DATE, this::markSessionCompletionDate)
                // mark students for transfer
            .end(MARK_SESSION_COMPLETION_DATE, COMPLETION_DATE_SET);
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
        log.debug("Posted completion message for saga step generateXAMFilesAndUpload: {}", saga.getSagaId());
    }

    private void markStagedStudentsReadyForTransfer(Event event, AssessmentSagaEntity saga, ApprovalSagaData approvalSagaData) throws JsonProcessingException {
        final AssessmentSagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(MARK_STAGED_STUDENTS_READY_FOR_TRANSFER.toString());
        saga.setStatus(SagaStatusEnum.IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        UUID sessionID = UUID.fromString(approvalSagaData.getSessionID());
        assessmentSessionRepository.findById(sessionID).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class));
        assessmentStudentService.markStagedStudentsReadyForTransfer();

        final Event nextEvent = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(MARK_STAGED_STUDENTS_READY_FOR_TRANSFER)
                .eventOutcome(STAGED_STUDENTS_MARKED_READY_FOR_TRANSFER)
                .eventPayload(JsonUtil.getJsonStringFromObject(approvalSagaData))
                .build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        log.debug("Posted completion message for saga step markStagedStudentsReadyForTransfer: {}", saga.getSagaId());
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

    // todo remove and  push to transfer orchestration as last step
    private void notifyGradOfUpdatedStudents(Event event, AssessmentSagaEntity saga, ApprovalSagaData approvalSagaData) throws JsonProcessingException {
        final AssessmentSagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(NOTIFY_GRAD_OF_UPDATED_STUDENTS.toString());
        saga.setStatus(SagaStatusEnum.IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
        
        UUID sessionID = UUID.fromString(approvalSagaData.getSessionID());
        var pair = sessionApprovalOrchestrationService.getStudentRegistrationEvents(sessionID);
        pair.getLeft().forEach(publisher::dispatchChoreographyEvent);
        final Event nextEvent = Event.builder().sagaId(saga.getSagaId())
                .eventType(NOTIFY_GRAD_OF_UPDATED_STUDENTS).eventOutcome(GRAD_STUDENT_API_NOTIFIED)
                .eventPayload(JsonUtil.getJsonStringFromObject(approvalSagaData))
                .build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        log.debug("Posted completion message for saga step notifyGradOfUpdatedStudents: {}", saga.getSagaId());
    }

    private void notifyMyEDOfApproval(Event event, AssessmentSagaEntity saga, ApprovalSagaData approvalSagaData) throws JsonProcessingException {
        final AssessmentSagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(NOTIFY_MYED_OF_UPDATED_STUDENTS.toString());
        saga.setStatus(SagaStatusEnum.IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        UUID sessionID = UUID.fromString(approvalSagaData.getSessionID());
        var session = assessmentSessionRepository.findById(sessionID).orElseThrow(() -> new EntityNotFoundException(AssessmentSessionEntity.class, "assessmentSessionID", sessionID.toString()));
        
        var emailFields = new HashMap<String, String>();
        emailFields.put("currentSession", session.getCourseYear() + "/" + session.getCourseMonth());

        var subject = emailProperties.getEmailSubjectMyEdApproval();
        var fromEmail = emailProperties.getEmailMyEdApprovalFrom();
        var toEmail = Arrays.asList(emailProperties.getEmailMyEdApprovalTo());

        var emailSagaData = emailService.createEmailSagaData(fromEmail, toEmail, subject, "myed.approval.notification", emailFields);
        emailService.sendEmail(emailSagaData);
        
        final Event nextEvent = Event.builder().sagaId(saga.getSagaId())
                .eventType(NOTIFY_MYED_OF_UPDATED_STUDENTS).eventOutcome(MYED_NOTIFIED)
                .eventPayload(JsonUtil.getJsonStringFromObject(approvalSagaData))
                .build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        log.debug("Posted completion message for saga step notifyGradOfUpdatedStudents: {}", saga.getSagaId());
    }

    public void startXamFileGenerationSaga(UUID sessionID) throws JsonProcessingException {
        var approvalSaga = ApprovalSagaData.builder().sessionID(sessionID.toString()).build();
        String payload = JsonUtil.getJsonStringFromObject(approvalSaga);
        AssessmentSagaEntity saga = sagaService.createSagaRecordInDB(
                this.getSagaName(),
                "system",
                payload,
                sessionID,
                null
        );
        this.startSaga(saga);
    }
}
