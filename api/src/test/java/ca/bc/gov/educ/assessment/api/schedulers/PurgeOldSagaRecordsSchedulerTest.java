package ca.bc.gov.educ.assessment.api.schedulers;

import ca.bc.gov.educ.assessment.api.BaseAssessmentAPITest;
import ca.bc.gov.educ.assessment.api.constants.EventType;
import ca.bc.gov.educ.assessment.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSagaEntity;
import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSagaEventStatesEntity;
import ca.bc.gov.educ.assessment.api.properties.ApplicationProperties;
import ca.bc.gov.educ.assessment.api.repository.v1.SagaEventRepository;
import ca.bc.gov.educ.assessment.api.repository.v1.SagaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PurgeOldSagaRecordsSchedulerTest extends BaseAssessmentAPITest {

  @Autowired
  SagaRepository repository;

  @Autowired
  SagaEventRepository sagaEventRepository;

  @Autowired
  PurgeOldSagaRecordsScheduler purgeOldSagaRecordsScheduler;

  @Test
  void pollSagaTableAndPurgeOldRecords_givenOldRecordsPresent_shouldBeDeleted() {
    final var payload = "{\"createUser\": \"ADMIN\", \"updateUser\": \"ADMIN\"}";
    final var saga = this.getSaga(payload);
    saga.setStatus("COMPLETED");
    this.repository.save(saga);
    this.sagaEventRepository.save(this.getSagaEvent(saga,payload));
    this.purgeOldSagaRecordsScheduler.setSagaRecordStaleInDays(0);
    this.purgeOldSagaRecordsScheduler.pollSagaTableAndPurgeOldRecords();
    final var sagas = this.repository.findAll();
    assertThat(sagas).isEmpty();
  }


  private AssessmentSagaEntity getSaga(final String payload) {
    return AssessmentSagaEntity
        .builder()
        .payload(payload)
        .assessmentStudentID(UUID.randomUUID())
        .sagaName("ASSESSMENT_TEST_SAGA")
        .status(SagaStatusEnum.STARTED.toString())
        .sagaState(EventType.INITIATED.toString())
        .createDate(LocalDateTime.now())
        .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
        .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
        .updateDate(LocalDateTime.now())
        .build();
  }
  private AssessmentSagaEventStatesEntity getSagaEvent(final AssessmentSagaEntity saga, final String payload) {
    return AssessmentSagaEventStatesEntity
        .builder()
        .sagaEventResponse(payload)
        .saga(saga)
        .sagaEventState("NOTIFY_ASSESSMENT_SAGA_RETURN")
        .sagaStepNumber(4)
        .sagaEventOutcome("ASSESSMENT_NOTIFIED")
        .createDate(LocalDateTime.now())
        .createUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
        .updateUser(ApplicationProperties.STUDENT_ASSESSMENT_API)
        .updateDate(LocalDateTime.now())
        .build();
  }
}
