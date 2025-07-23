package ca.bc.gov.educ.assessment.api.orchestrator;

import ca.bc.gov.educ.assessment.api.constants.SagaEnum;
import ca.bc.gov.educ.assessment.api.constants.TopicsEnum;
import ca.bc.gov.educ.assessment.api.messaging.MessagePublisher;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSagaEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSagaEventStatesEntity;
import ca.bc.gov.educ.assessment.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.assessment.api.service.v1.SagaService;
import ca.bc.gov.educ.assessment.api.service.v1.StudentAssessmentResultService;
import ca.bc.gov.educ.assessment.api.struct.Event;
import ca.bc.gov.educ.assessment.api.struct.v1.StudentResultSagaData;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;

import static ca.bc.gov.educ.assessment.api.constants.EventOutcome.STUDENT_RESULT_CREATED;
import static ca.bc.gov.educ.assessment.api.constants.EventType.CREATE_STUDENT_RESULT;
import static ca.bc.gov.educ.assessment.api.constants.SagaStatusEnum.IN_PROGRESS;


@Component
@Slf4j
public class StudentResultProcessingOrchestrator extends BaseOrchestrator<StudentResultSagaData> {
  private final StudentAssessmentResultService studentAssessmentResultService;

  protected StudentResultProcessingOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, StudentAssessmentResultService studentAssessmentResultService) {
    super(sagaService, messagePublisher, StudentResultSagaData.class, SagaEnum.PROCESS_STUDENT_RESULT.toString(), TopicsEnum.PROCESS_STUDENT_RESULT_SAGA_TOPIC.toString());
      this.studentAssessmentResultService = studentAssessmentResultService;
  }

  @Override
  public void populateStepsToExecuteMap() {
    this.stepBuilder()
            .begin(CREATE_STUDENT_RESULT, this::createStudentResultRecord)
            .end(CREATE_STUDENT_RESULT, STUDENT_RESULT_CREATED);
  }

  public void createStudentResultRecord(final Event event, final AssessmentSagaEntity saga, final StudentResultSagaData studentResultSagaData) {
    final AssessmentSagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(CREATE_STUDENT_RESULT.toString());
    saga.setStatus(IN_PROGRESS.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    final Event.EventBuilder eventBuilder = Event.builder();
    eventBuilder.sagaId(saga.getSagaId()).eventType(CREATE_STUDENT_RESULT);

    studentAssessmentResultService.processLoadedRecordsInBatchFile(studentResultSagaData);

    eventBuilder.eventOutcome(STUDENT_RESULT_CREATED);
    val nextEvent = eventBuilder.build();
    this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
    log.debug("message sent to {} for {} Event. :: {}", this.getTopicToSubscribe(), nextEvent, saga.getSagaId());
  }

}
