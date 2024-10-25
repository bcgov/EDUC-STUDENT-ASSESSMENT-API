package ca.bc.gov.educ.eas.api.orchestrator;

import ca.bc.gov.educ.eas.api.constants.SagaEnum;
import ca.bc.gov.educ.eas.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.eas.api.constants.TopicsEnum;
import ca.bc.gov.educ.eas.api.messaging.MessagePublisher;
import ca.bc.gov.educ.eas.api.model.v1.EasSagaEntity;
import ca.bc.gov.educ.eas.api.model.v1.SagaEventStatesEntity;
import ca.bc.gov.educ.eas.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.eas.api.service.v1.SagaService;
import ca.bc.gov.educ.eas.api.service.v1.StudentRegistrationOrchestrationService;
import ca.bc.gov.educ.eas.api.struct.Event;
import ca.bc.gov.educ.eas.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.eas.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static ca.bc.gov.educ.eas.api.constants.EventOutcome.STUDENT_REGISTRATION_PUBLISHED;
import static ca.bc.gov.educ.eas.api.constants.EventType.PUBLISH_STUDENT_REGISTRATION;


@Component
@Slf4j
public class StudentRegistrationOrchestrator extends BaseOrchestrator<AssessmentStudent> {

    private final StudentRegistrationOrchestrationService studentRegistrationOrchestrationService;

    protected StudentRegistrationOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, StudentRegistrationOrchestrationService studentRegistrationOrchestrationService) {
        super(sagaService, messagePublisher, AssessmentStudent.class, SagaEnum.PUBLISH_STUDENT_REGISTRATION.toString(), TopicsEnum.PUBLISH_STUDENT_REGISTRATION_SAGA_TOPIC.toString());
        this.studentRegistrationOrchestrationService = studentRegistrationOrchestrationService;
    }

    @Override
    public void populateStepsToExecuteMap() {
        this.stepBuilder()
                .begin(PUBLISH_STUDENT_REGISTRATION, this::publishStudentRegistration)
                .end(PUBLISH_STUDENT_REGISTRATION, STUDENT_REGISTRATION_PUBLISHED);
    }

    private void publishStudentRegistration(Event event, EasSagaEntity saga, AssessmentStudent sagaData) throws JsonProcessingException {
        final SagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
        saga.setSagaState(PUBLISH_STUDENT_REGISTRATION.toString());
        saga.setStatus(SagaStatusEnum.IN_PROGRESS.toString());
        this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);
        //Service call
        studentRegistrationOrchestrationService.publishStudentRegistration(sagaData, saga);
        final Event nextEvent = Event.builder().sagaId(saga.getSagaId())
                .eventType(PUBLISH_STUDENT_REGISTRATION).eventOutcome(STUDENT_REGISTRATION_PUBLISHED)
                .eventPayload(JsonUtil.getJsonStringFromObject(sagaData))
                .build();
        this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
        logMessage(nextEvent, saga);
    }

    private void logMessage(Event event, EasSagaEntity saga) {
        log.debug("message sent to {} for {} Event. :: {}", this.getTopicToSubscribe(), event, saga.getSagaId());
    }

}
