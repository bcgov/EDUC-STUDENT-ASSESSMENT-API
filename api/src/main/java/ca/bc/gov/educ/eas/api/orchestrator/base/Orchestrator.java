package ca.bc.gov.educ.eas.api.orchestrator.base;

import ca.bc.gov.educ.eas.api.model.v1.EasSagaEntity;

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
  void startSaga(EasSagaEntity saga);

  /**
   * create saga.
   *
   * @param payload   the payload
   * @param userName  the user who created the saga
   * @return the saga
   */
  EasSagaEntity createSaga(String payload, String userName, UUID assessmentStudentID);

  /**
   * Replay saga.
   *
   * @param saga the saga
   * @throws IOException          the io exception
   * @throws InterruptedException the interrupted exception
   * @throws TimeoutException     the timeout exception
   */
  void replaySaga(EasSagaEntity saga) throws IOException, InterruptedException, TimeoutException;
}
