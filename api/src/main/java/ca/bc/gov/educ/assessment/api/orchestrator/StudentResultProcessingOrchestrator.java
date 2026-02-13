package ca.bc.gov.educ.assessment.api.orchestrator;

import ca.bc.gov.educ.assessment.api.constants.SagaEnum;
import ca.bc.gov.educ.assessment.api.constants.TopicsEnum;
import ca.bc.gov.educ.assessment.api.messaging.MessagePublisher;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSagaEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSagaEventStatesEntity;
import ca.bc.gov.educ.assessment.api.orchestrator.base.BaseOrchestrator;
import ca.bc.gov.educ.assessment.api.service.v1.DOARStagingReportService;
import ca.bc.gov.educ.assessment.api.service.v1.SagaService;
import ca.bc.gov.educ.assessment.api.service.v1.StudentAssessmentResultService;
import ca.bc.gov.educ.assessment.api.struct.Event;
import ca.bc.gov.educ.assessment.api.struct.v1.StudentResultSagaData;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.stereotype.Component;

import static ca.bc.gov.educ.assessment.api.constants.EventOutcome.*;
import static ca.bc.gov.educ.assessment.api.constants.EventType.*;
import static ca.bc.gov.educ.assessment.api.constants.SagaStatusEnum.IN_PROGRESS;


@Component
@Slf4j
public class StudentResultProcessingOrchestrator extends BaseOrchestrator<StudentResultSagaData> {
  private final StudentAssessmentResultService studentAssessmentResultService;
  private final DOARStagingReportService doarStagingReportService;

  protected StudentResultProcessingOrchestrator(final SagaService sagaService, final MessagePublisher messagePublisher, StudentAssessmentResultService studentAssessmentResultService, DOARStagingReportService doarStagingReportService) {
    super(sagaService, messagePublisher, StudentResultSagaData.class, SagaEnum.PROCESS_STUDENT_RESULT.toString(), TopicsEnum.PROCESS_STUDENT_RESULT_SAGA_TOPIC.toString());
      this.studentAssessmentResultService = studentAssessmentResultService;
      this.doarStagingReportService = doarStagingReportService;
  }

  @Override
  public void populateStepsToExecuteMap() {
    this.stepBuilder()
            .begin(FIND_STUDENT_IN_GRAD_OR_ADOPT, this::findStudentInGradOrAdoptRecord)
            .step(FIND_STUDENT_IN_GRAD_OR_ADOPT, FIND_STUDENT_IN_GRAD_OR_ADOPT_COMPLETED, CREATE_STUDENT_RESULT, this::createStudentResultRecord)
            .step(CREATE_STUDENT_RESULT, STUDENT_RESULT_CREATED, CALCULATE_STAGED_STUDENT_DOAR, this::createAndPopulateDOARCalculations)
            .end(CALCULATE_STAGED_STUDENT_DOAR, STAGED_STUDENT_DOAR_CALCULATED);
  }

  public void findStudentInGradOrAdoptRecord(final Event event, final AssessmentSagaEntity saga, final StudentResultSagaData studentResultSagaData) {
    final AssessmentSagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(FIND_STUDENT_IN_GRAD_OR_ADOPT.toString());
    saga.setStatus(IN_PROGRESS.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    final Event.EventBuilder eventBuilder = Event.builder();
    eventBuilder.sagaId(saga.getSagaId()).eventType(FIND_STUDENT_IN_GRAD_OR_ADOPT);

    studentAssessmentResultService.findGradStudentRecordOrCreate(studentResultSagaData);

    eventBuilder.eventOutcome(FIND_STUDENT_IN_GRAD_OR_ADOPT_COMPLETED);
    val nextEvent = eventBuilder.build();
    this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
    log.debug("message sent to {} for {} Event. :: {}", this.getTopicToSubscribe(), nextEvent, saga.getSagaId());
  }

  public void createStudentResultRecord(final Event event, final AssessmentSagaEntity saga, final StudentResultSagaData studentResultSagaData) {
    final AssessmentSagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(CREATE_STUDENT_RESULT.toString());
    saga.setStatus(IN_PROGRESS.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    final Event.EventBuilder eventBuilder = Event.builder();
    eventBuilder.sagaId(saga.getSagaId()).eventType(CREATE_STUDENT_RESULT);

    studentAssessmentResultService.processLoadedResultRecord(studentResultSagaData);

    eventBuilder.eventOutcome(STUDENT_RESULT_CREATED);
    val nextEvent = eventBuilder.build();
    this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
    log.debug("message sent to {} for {} Event. :: {}", this.getTopicToSubscribe(), nextEvent, saga.getSagaId());
  }

  protected void createAndPopulateDOARCalculations(Event event, AssessmentSagaEntity saga, StudentResultSagaData studentResultSagaData) {
    final AssessmentSagaEventStatesEntity eventStates = this.createEventState(saga, event.getEventType(), event.getEventOutcome(), event.getEventPayload());
    saga.setSagaState(CALCULATE_STAGED_STUDENT_DOAR.toString());
    saga.setStatus(IN_PROGRESS.toString());
    this.getSagaService().updateAttachedSagaWithEvents(saga, eventStates);

    final Event.EventBuilder eventBuilder = Event.builder();
    eventBuilder.sagaId(saga.getSagaId()).eventType(CALCULATE_STAGED_STUDENT_DOAR);

    doarStagingReportService.createAndPopulateDOARStagingSummaryCalculations(studentResultSagaData);

    eventBuilder.eventOutcome(STAGED_STUDENT_DOAR_CALCULATED);
    val nextEvent = eventBuilder.build();
    this.postMessageToTopic(this.getTopicToSubscribe(), nextEvent);
    log.debug("message sent to {} for {} Event. :: {}", this.getTopicToSubscribe(), nextEvent, saga.getSagaId());
  }

}
