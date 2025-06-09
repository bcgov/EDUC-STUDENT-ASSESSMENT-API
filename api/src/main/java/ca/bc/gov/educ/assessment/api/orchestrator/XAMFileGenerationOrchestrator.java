package ca.bc.gov.educ.assessment.api.orchestrator;

import ca.bc.gov.educ.assessment.api.constants.EventOutcome;
import ca.bc.gov.educ.assessment.api.constants.EventType;
import ca.bc.gov.educ.assessment.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.assessment.api.messaging.MessagePublisher;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSagaEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSagaEventStatesEntity;
import ca.bc.gov.educ.assessment.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.assessment.api.service.v1.SagaService;
import ca.bc.gov.educ.assessment.api.service.v1.XAMFileService;
import ca.bc.gov.educ.assessment.api.struct.Event;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static ca.bc.gov.educ.assessment.api.constants.SagaEnum.GENERATE_XAM_FILES;
import static ca.bc.gov.educ.assessment.api.constants.TopicsEnum.XAM_FILE_GENERATION_TOPIC;

@Slf4j
@Component
public class XAMFileGenerationOrchestrator extends BaseOrchestrator<String> {

    private final XAMFileService xamFileService;
    private final SagaService sagaService;

    protected XAMFileGenerationOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, final XAMFileService xamFileService) {
        super(sagaService, messagePublisher, String.class, GENERATE_XAM_FILES.toString(), XAM_FILE_GENERATION_TOPIC.toString());
        this.xamFileService = xamFileService;
        this.sagaService = sagaService;
    }

    @Override
    public void populateStepsToExecuteMap() {
        this.stepBuilder()
            .begin(EventType.GENERATE_XAM_FILES_AND_UPLOAD, this::generateXAMFilesAndUpload)
            .end(EventType.GENERATE_XAM_FILES_AND_UPLOAD, EventOutcome.XAM_FILES_GENERATED_AND_UPLOADED);
    }

    private void generateXAMFilesAndUpload(Event event, AssessmentSagaEntity saga, String payload) throws JsonProcessingException {
        final AssessmentSagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(EventType.GENERATE_XAM_FILES_AND_UPLOAD.toString());
        saga.setStatus(SagaStatusEnum.IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        UUID sessionID = UUID.fromString(payload);
        xamFileService.generateAndUploadXamFiles(sessionID);

        final Event nextEvent = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(EventType.GENERATE_XAM_FILES_AND_UPLOAD)
                .eventOutcome(EventOutcome.XAM_FILES_GENERATED_AND_UPLOADED)
                .eventPayload(JsonUtil.getJsonStringFromObject("All schools processed"))
                .build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        log.debug("Posted completion message for saga: {}", saga.getSagaId());
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
