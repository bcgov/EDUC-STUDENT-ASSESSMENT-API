package ca.bc.gov.educ.assessment.api.repository.v1;

import ca.bc.gov.educ.assessment.api.model.v1.AssessmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssessmentRepository extends JpaRepository<AssessmentEntity, UUID> {

    @Query("SELECT a FROM AssessmentEntity a WHERE a.assessmentSessionEntity.sessionID = :sessionID")
    Optional<List<AssessmentEntity>> findBySessionID(UUID sessionID);

    Optional<AssessmentEntity> findByAssessmentSessionEntity_SessionIDAndAssessmentTypeCode(UUID sessionID, String assessmentTypeCode);
}
