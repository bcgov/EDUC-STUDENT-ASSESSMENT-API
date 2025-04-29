package ca.bc.gov.educ.assessment.api.orchestrator;

import ca.bc.gov.educ.assessment.api.constants.SagaEnum;
import ca.bc.gov.educ.assessment.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.assessment.api.constants.TopicsEnum;
import ca.bc.gov.educ.assessment.api.messaging.MessagePublisher;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSagaEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSagaEventStatesEntity;
import ca.bc.gov.educ.assessment.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.assessment.api.service.v1.SagaService;
import ca.bc.gov.educ.assessment.api.service.v1.StudentRegistrationOrchestrationService;
import ca.bc.gov.educ.assessment.api.struct.Event;
import ca.bc.gov.educ.assessment.api.struct.v1.AssessmentStudent;
import ca.bc.gov.educ.assessment.api.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static ca.bc.gov.educ.assessment.api.constants.EventOutcome.STUDENT_REGISTRATION_PUBLISHED;
import static ca.bc.gov.educ.assessment.api.constants.EventType.PUBLISH_STUDENT_REGISTRATION;


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

    private void publishStudentRegistration(Event event, AssessmentSagaEntity saga, AssessmentStudent sagaData) throws JsonProcessingException {
        final AssessmentSagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
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

    private void logMessage(Event event, AssessmentSagaEntity saga) {
        log.debug("message sent to {} for {} Event. :: {}", this.getTopicToSubscribe(), event, saga.getSagaId());
    }

}
