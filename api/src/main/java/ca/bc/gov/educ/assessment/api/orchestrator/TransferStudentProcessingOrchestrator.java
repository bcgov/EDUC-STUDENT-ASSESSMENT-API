package ca.bc.gov.educ.assessment.api.orchestrator;

import ca.bc.gov.educ.assessment.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.assessment.api.messaging.MessagePublisher;
import ca.bc.gov.educ.assessment.api.messaging.jetstream.Publisher;
import ca.bc.gov.educ.assessment.api.model.v1.*;
import ca.bc.gov.educ.assessment.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.assessment.api.service.v1.DOARReportService;
import ca.bc.gov.educ.assessment.api.service.v1.SagaService;
import ca.bc.gov.educ.assessment.api.service.v1.TransferStudentOrchestrationService;
import ca.bc.gov.educ.assessment.api.struct.Event;
import ca.bc.gov.educ.assessment.api.struct.v1.TransferOnApprovalSagaData;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static ca.bc.gov.educ.assessment.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.assessment.api.constants.EventType.*;
import static ca.bc.gov.educ.assessment.api.constants.SagaEnum.PROCESS_STUDENT_TRANSFER;
import static ca.bc.gov.educ.assessment.api.constants.SagaStatusEnum.IN_PROGRESS;
import static ca.bc.gov.educ.assessment.api.constants.TopicsEnum.STUDENT_TRANSFER_PROCESSING_TOPIC;

@Slf4j
@Component
public class TransferStudentProcessingOrchestrator extends BaseOrchestrator<TransferOnApprovalSagaData> {

    private final SagaService sagaService;
    private final DOARReportService doarReportService;
    private final TransferStudentOrchestrationService transferStudentOrchestrationService;
    private final Publisher publisher;

    protected TransferStudentProcessingOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, DOARReportService doarReportService, TransferStudentOrchestrationService transferStudentOrchestrationService, Publisher publisher) {
        super(sagaService, messagePublisher, TransferOnApprovalSagaData.class, PROCESS_STUDENT_TRANSFER.toString(), STUDENT_TRANSFER_PROCESSING_TOPIC.toString());
        this.sagaService = sagaService;
        this.doarReportService = doarReportService;
        this.transferStudentOrchestrationService = transferStudentOrchestrationService;
        this.publisher = publisher;
    }

    @Override
    public void populateStepsToExecuteMap() {
        this.stepBuilder()
            .begin(PROCESS_STUDENT_TRANSFER_EVENT, this::processStudentTransfer)
            .step(PROCESS_STUDENT_TRANSFER_EVENT, STUDENT_TRANSFER_PROCESSED, CALCULATE_STUDENT_DOAR, this::createAndPopulateDOARCalculations)
            .step(CALCULATE_STUDENT_DOAR, STUDENT_DOAR_CALCULATED, NOTIFY_GRAD_OF_UPDATED_STUDENTS, this::notifyGradOfUpdatedStudents)
            .step(NOTIFY_GRAD_OF_UPDATED_STUDENTS, GRAD_STUDENT_API_NOTIFIED, DELETE_FROM_STAGING, this::deleteStagingRecord)
            .end(DELETE_FROM_STAGING, DELETED_STUDENT_FROM_STAGING);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void processStudentTransfer(Event event, AssessmentSagaEntity saga, TransferOnApprovalSagaData transferOnApprovalSagaData) throws JsonProcessingException {
        final AssessmentSagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(PROCESS_STUDENT_TRANSFER_EVENT.toString());
        saga.setStatus(SagaStatusEnum.IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        UUID stagedStudentAssessmentId = UUID.fromString(transferOnApprovalSagaData.getStagedStudentAssessmentID());
        log.debug("Processing transfer for staged student assessment: {}", stagedStudentAssessmentId);

        // Transfer student data from staging to main tables and delete from staging (same transaction)
        transferStudentOrchestrationService.transferStagedStudentToMainTables(stagedStudentAssessmentId);

        final Event nextEvent = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(PROCESS_STUDENT_TRANSFER_EVENT)
                .eventOutcome(STUDENT_TRANSFER_PROCESSED)
                .eventPayload(JsonUtil.getJsonStringFromObject(transferOnApprovalSagaData))
                .build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        log.debug("Posted completion message for saga step processStudentTransfer: {}", saga.getSagaId());
    }

    protected void createAndPopulateDOARCalculations(Event event, AssessmentSagaEntity saga, TransferOnApprovalSagaData transferOnApprovalSagaData) {
        final AssessmentSagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(CALCULATE_STUDENT_DOAR.toString());
        saga.setStatus(IN_PROGRESS.toString());
        log.info("MarcoX10");
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
        log.info("MarcoX11");
        final Event.EventBuilder eventBuilder = Event.builder();
        eventBuilder.sagaId(saga.getSagaId()).eventType(CALCULATE_STUDENT_DOAR);

        doarReportService.createAndPopulateDOARSummaryCalculations(transferOnApprovalSagaData);

        eventBuilder.eventOutcome(STUDENT_DOAR_CALCULATED);
        val nextEvent = eventBuilder.build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        log.debug("message sent to {} for {} Event. :: {}", this.getTopicToSubscribe(), nextEvent, saga.getSagaId());
    }

    protected void deleteStagingRecord(Event event, AssessmentSagaEntity saga, TransferOnApprovalSagaData transferOnApprovalSagaData) {
        final AssessmentSagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(DELETE_FROM_STAGING.toString());
        saga.setStatus(IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        final Event.EventBuilder eventBuilder = Event.builder();
        eventBuilder.sagaId(saga.getSagaId()).eventType(DELETE_FROM_STAGING);

        transferStudentOrchestrationService.deleteFromStagingTable(UUID.fromString(transferOnApprovalSagaData.getStagedStudentAssessmentID()));

        eventBuilder.eventOutcome(DELETED_STUDENT_FROM_STAGING);
        val nextEvent = eventBuilder.build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        log.debug("message sent to {} for {} Event. :: {}", this.getTopicToSubscribe(), nextEvent, saga.getSagaId());
    }

    private void notifyGradOfUpdatedStudents(Event event, AssessmentSagaEntity saga, TransferOnApprovalSagaData transferOnApprovalSagaData) throws JsonProcessingException {
        final AssessmentSagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(NOTIFY_GRAD_OF_UPDATED_STUDENTS.toString());
        saga.setStatus(SagaStatusEnum.IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        var pair = transferStudentOrchestrationService.getStudentRegistrationEvents(UUID.fromString(transferOnApprovalSagaData.getStudentID()));
        pair.getLeft().forEach(publisher::dispatchChoreographyEvent);
        final Event nextEvent = Event.builder().sagaId(saga.getSagaId())
                .eventType(NOTIFY_GRAD_OF_UPDATED_STUDENTS).eventOutcome(GRAD_STUDENT_API_NOTIFIED)
                .eventPayload(JsonUtil.getJsonStringFromObject(transferOnApprovalSagaData))
                .build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        log.debug("Posted completion message for saga step notifyGradOfUpdatedStudents: {}", saga.getSagaId());
    }
}
