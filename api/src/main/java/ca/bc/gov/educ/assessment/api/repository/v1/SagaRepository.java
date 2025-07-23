package ca.bc.gov.educ.assessment.api.repository.v1;


import ca.bc.gov.educ.assessment.api.model.v1.AssessmentSagaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The interface Saga repository.
 */
@Repository
public interface SagaRepository extends JpaRepository<AssessmentSagaEntity, UUID>, JpaSpecificationExecutor<AssessmentSagaEntity> {

  List<AssessmentSagaEntity> findTop500ByStatusInOrderByCreateDate(List<String> statuses);

  Optional<AssessmentSagaEntity> findByAssessmentStudentIDAndSagaNameAndStatusNot(UUID assessmentStudentID, String sagaName, String status);

  List<AssessmentSagaEntity> findByStagedStudentResultIDAndSagaNameAndStatusNot(UUID stagedStudentResultID, String sagaName, String status);

  @Transactional
  @Modifying
  @Query("delete from AssessmentSagaEntity where createDate <= :createDate")
  void deleteByCreateDateBefore(LocalDateTime createDate);

  @Query(value = "SELECT s.SAGA_ID FROM ASSESSMENT_SAGA s WHERE s.STATUS in :cleanupStatus LIMIT :batchSize", nativeQuery = true)
  List<UUID> findByStatusIn(List<String> cleanupStatus, int batchSize);
}
