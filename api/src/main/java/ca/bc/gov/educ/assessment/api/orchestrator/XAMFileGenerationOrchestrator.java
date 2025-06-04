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
import ca.bc.gov.educ.assessment.api.struct.external.institute.v1.SchoolTombstone;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

import static ca.bc.gov.educ.assessment.api.constants.SagaEnum.GENERATE_XAM_FILES;
import static ca.bc.gov.educ.assessment.api.constants.TopicsEnum.XAM_FILE_GENERATION_TOPIC;

@Slf4j
@Component
public class XAMFileGenerationOrchestrator extends BaseOrchestrator<List<SchoolTombstone>> {

    private final XAMFileService xamFileService;

    protected XAMFileGenerationOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, final XAMFileService xamFileService) {
        super(sagaService, messagePublisher, (Class<List<SchoolTombstone>>) (Class<?>) List.class, GENERATE_XAM_FILES.toString(), XAM_FILE_GENERATION_TOPIC.toString());
        this.xamFileService = xamFileService;
    }

    @Override
    public void populateStepsToExecuteMap() {
        this.stepBuilder()
                .begin(EventType.GENERATE_XAM_FILES, this::generateXAMFiles)
                .end(EventType.GENERATE_XAM_FILES, EventOutcome.XAM_FILES_GENERATED);
    }

    private void generateXAMFiles(Event event, AssessmentSagaEntity saga, List<SchoolTombstone> schools) throws JsonProcessingException {
        // todo probably want some logic in here to get the schools to generate for
        final AssessmentSagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(EventType.GENERATE_XAM_FILES.toString());
        saga.setStatus(SagaStatusEnum.IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

        log.info("Generating XAM files for schools...");

        xamFileService.generateXamFilesForSchools(saga.getAssessmentStudentID(), schools);

        final Event nextEvent = Event.builder()
                .sagaId(saga.getSagaId())
                .eventType(EventType.GENERATE_XAM_FILES)
                .eventOutcome(EventOutcome.XAM_FILES_GENERATED)
                .eventPayload(JsonUtil.getJsonStringFromObject(schools))
                .build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        logMessage(nextEvent, saga);
    }

    private void logMessage(Event event, AssessmentSagaEntity saga) {
        log.debug("Message sent to {} for {} Event. :: {}", this.getTopicToSubscribe(), event, saga.getSagaId());
    }
}
