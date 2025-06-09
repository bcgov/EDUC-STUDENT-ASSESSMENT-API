package ca.bc.gov.educ.assessment.api.orchestrator;

import ca.bc.gov.educ.assessment.api.constants.EventOutcome;
import ca.bc.gov.educ.assessment.api.constants.EventType;
import ca.bc.gov.educ.assessment.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.assessment.api.messaging.MessagePublisher;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSagaEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSagaEventStatesEntity;
import ca.bc.gov.educ.assessment.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.assessment.api.rest.RestUtils;
import ca.bc.gov.educ.assessment.api.service.v1.SagaService;
import ca.bc.gov.educ.assessment.api.service.v1.XAMFileService;
import ca.bc.gov.educ.assessment.api.struct.Event;
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static ca.bc.gov.educ.assessment.api.constants.SagaEnum.GENERATE_XAM_FILES;
import static ca.bc.gov.educ.assessment.api.constants.TopicsEnum.XAM_FILE_GENERATION_TOPIC;

@Slf4j
@Component
public class XAMFileGenerationOrchestrator extends BaseOrchestrator<SchoolTombstone> {

    private final XAMFileService xamFileService;
    private final SagaService sagaService;
    private final RestUtils restUtils;

    protected XAMFileGenerationOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, final XAMFileService xamFileService, final RestUtils restUtils) {
        super(sagaService, messagePublisher, SchoolTombstone.class, GENERATE_XAM_FILES.toString(), XAM_FILE_GENERATION_TOPIC.toString());
        this.xamFileService = xamFileService;
        this.sagaService = sagaService;
        this.restUtils = restUtils;
    }

    @Override
    public void populateStepsToExecuteMap() {
        // IMPORTANT: Both generate and upload steps must run on the same pod instance,
        // as the generated file is only available locally.
        // Cannot split these steps across different pods.
        // Generate and immediately upload in the same execution context
        this.stepBuilder()
            .begin(EventType.GENERATE_XAM_AND_UPLOAD_FILE, this::generateXAMFileAndUpload)
            .end(EventType.GENERATE_XAM_AND_UPLOAD_FILE, EventOutcome.XAM_FILE_GENERATED_AND_UPLOADED);
    }

    // todo put in the controller where this is being called from (by endpoint?)
    public void startXamFileGenerationSagas(UUID sessionID, String userName) {
        List<SchoolTombstone> schools = restUtils.getAllSchoolTombstones();
        for (SchoolTombstone school : schools) {
            try {
                String payload = JsonUtil.getJsonStringFromObject(school);
                AssessmentSagaEntity saga = sagaService.createSagaRecordInDB(
                        this.getSagaName(),
                        userName,
                        payload,
                        sessionID
                );
                this.startSaga(saga);
            } catch (Exception e) {
                log.error("Failed to start XAM file generation saga for school: {}", school.getMincode(), e);
            }
        }
    }

    private void generateXAMFileAndUpload(Event event, AssessmentSagaEntity saga, SchoolTombstone school) throws JsonProcessingException {
        final AssessmentSagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(EventType.GENERATE_XAM_AND_UPLOAD_FILE.toString());
        saga.setStatus(SagaStatusEnum.IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        log.info("Generating and uploading XAM file for school: {}", school.getMincode());

        String filePath = xamFileService.generateXamFileAndReturnPath(saga.getAssessmentStudentID(), school);
        xamFileService.uploadFilePathToS3(filePath, saga.getAssessmentStudentID(), school);

        final Event nextEvent = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(EventType.GENERATE_XAM_AND_UPLOAD_FILE)
                .eventOutcome(EventOutcome.XAM_FILE_GENERATED_AND_UPLOADED)
                .eventPayload(JsonUtil.getJsonStringFromObject(school))
                .build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        log.debug("Message sent to {} for {} Event. :: {}", this.getTopicToSubscribe(), event, saga.getSagaId());
    }
}
