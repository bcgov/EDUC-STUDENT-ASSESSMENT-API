package ca.bc.gov.educ.eas.api.repository.v1;

import ca.bc.gov.educ.eas.api.model.v1.AssessmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AssessmentRepository extends JpaRepository<AssessmentEntity, UUID> {

    @Query("SELECT a FROM AssessmentEntity a WHERE a.sessionEntity.sessionID = :sessionID")
    Optional<List<AssessmentEntity>> findBySessionID(UUID sessionID);
}
