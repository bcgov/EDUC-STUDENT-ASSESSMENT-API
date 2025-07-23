package ca.bc.gov.educ.assessment.api.orchestrator.base;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSagaEntity;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

/**
 * The interface Orchestrator.
 */
public interface Orchestrator {


  /**
   * Gets saga name.
   *
   * @return the saga name
   */
  String getSagaName();

  /**
   * Start saga.
   *
   * @param saga  the saga data
   */
  void startSaga(AssessmentSagaEntity saga);

  /**
   * create saga.
   *
   * @param payload   the payload
   * @param userName  the user who created the saga
   * @return the saga
   */
  AssessmentSagaEntity createSaga(String payload, String userName, UUID assessmentStudentID, final UUID stagedStudentResultId);

  /**
   * Replay saga.
   *
   * @param saga the saga
   * @throws IOException          the io exception
   * @throws InterruptedException the interrupted exception
   * @throws TimeoutException     the timeout exception
   */
  void replaySaga(AssessmentSagaEntity saga) throws IOException, InterruptedException, TimeoutException;
}
