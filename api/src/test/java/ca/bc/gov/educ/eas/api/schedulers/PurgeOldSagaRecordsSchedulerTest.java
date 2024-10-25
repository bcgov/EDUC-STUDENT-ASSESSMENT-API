package ca.bc.gov.educ.eas.api.schedulers;

import ca.bc.gov.educ.eas.api.BaseEasAPITest;
import ca.bc.gov.educ.eas.api.constants.EventType;
import ca.bc.gov.educ.eas.api.constants.SagaStatusEnum;
import ca.bc.gov.educ.eas.api.model.v1.SagaEventStatesEntity;
import ca.bc.gov.educ.eas.api.model.v1.EasSagaEntity;
import ca.bc.gov.educ.eas.api.repository.v1.SagaEventRepository;
import ca.bc.gov.educ.eas.api.repository.v1.SagaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.UUID;

class PurgeOldSagaRecordsSchedulerTest extends BaseEasAPITest {

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
    this.repository.save(saga);
    this.sagaEventRepository.save(this.getSagaEvent(saga,payload));
    this.purgeOldSagaRecordsScheduler.setSagaRecordStaleInDays(0);
    this.purgeOldSagaRecordsScheduler.pollSagaTableAndPurgeOldRecords();
    final var sagas = this.repository.findAll();
    assertThat(sagas).isEmpty();
  }


  private EasSagaEntity getSaga(final String payload) {
    return EasSagaEntity
        .builder()
        .payload(payload)
        .assessmentStudentID(UUID.randomUUID())
        .sagaName("EAS_TEST_SAGA")
        .status(SagaStatusEnum.STARTED.toString())
        .sagaState(EventType.INITIATED.toString())
        .createDate(LocalDateTime.now())
        .createUser("EAS_API")
        .updateUser("EAS_API")
        .updateDate(LocalDateTime.now())
        .build();
  }
  private SagaEventStatesEntity getSagaEvent(final EasSagaEntity saga, final String payload) {
    return SagaEventStatesEntity
        .builder()
        .sagaEventResponse(payload)
        .saga(saga)
        .sagaEventState("NOTIFY_EAS_SAGA_RETURN")
        .sagaStepNumber(4)
        .sagaEventOutcome("EAS_NOTIFIED")
        .createDate(LocalDateTime.now())
        .createUser("EAS_API")
        .updateUser("EAS_API")
        .updateDate(LocalDateTime.now())
        .build();
  }
}
